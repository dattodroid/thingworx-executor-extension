package ext.sma.tw;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;

import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.ThingworxBaseTemplateDefinition;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinition;
import com.thingworx.metadata.annotations.ThingworxConfigurationTableDefinitions;
import com.thingworx.metadata.annotations.ThingworxDataShapeDefinition;
import com.thingworx.metadata.annotations.ThingworxFieldDefinition;
import com.thingworx.persistence.TransactionFactory;
import com.thingworx.security.context.SecurityContext;
import com.thingworx.system.ContextType;
import com.thingworx.things.Thing;
import com.thingworx.types.InfoTable;
import com.thingworx.types.collections.ValueCollection;
import com.thingworx.webservices.context.ThreadLocalContext;

@ThingworxConfigurationTableDefinitions(tables = {
		@ThingworxConfigurationTableDefinition(name = ServiceExecutorTemplate.CONFIG_TABLE_SETTINGS, description = "Settings for the Transactionable", isMultiRow = false, dataShape = @ThingworxDataShapeDefinition(fields = {
				@ThingworxFieldDefinition(name = ServiceExecutorTemplate.CONFIG_TABLE_TIMEOUT, description = "Service timeout in seconds", baseType = "INTEGER", aspects = {
						"defaultValue:30", "friendlyName:Service Execution Timeout" }, ordinal = 1),
				@ThingworxFieldDefinition(name = ServiceExecutorTemplate.CONFIG_TABLE_THREADCOUNT, description = "Worker Thread Count", baseType = "INTEGER", aspects = {
						"defaultValue:3", "friendlyName:ExecutorService Thread Count" }, ordinal = 2) })) })

@ThingworxBaseTemplateDefinition(name = "GenericThing")
public class ServiceExecutorTemplate extends Thing {

	private static final long serialVersionUID = 5395519862764674780L;

	static final String CONFIG_TABLE_SETTINGS = "executorSettings";
	static final String CONFIG_TABLE_TIMEOUT = "executorTimeout";
	static final String CONFIG_TABLE_THREADCOUNT = "executorThreadCount";

	// Logger instance
	protected static final Logger _logger = LogUtilities.getInstance()
			.getApplicationLogger(ServiceExecutorTemplate.class);

	private final AtomicReference<ExecutorService> _workerRef = new AtomicReference<>();

	public ServiceExecutorTemplate() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void startThing(ContextType contextType) throws Exception {
		// TODO Auto-generated method stub
		super.startThing(contextType);

		_logger.info("Starting {} ExecutorService", getName());
		final ExecutorService worker = Executors.newFixedThreadPool(getThreadCount());
		if (!_workerRef.compareAndSet(null, worker)) {
			_logger.warn( "{} ExecutorService already started", getName());
			return;
		}
	}

	@Override
	public void stopThing(ContextType contextType) {
		final ExecutorService worker = _workerRef.getAndSet(null);
		if (worker != null && !worker.isShutdown()) {
			_logger.warn("Shutting down {} ExecutorService", getName());
			worker.shutdownNow();
		}
	}

	private int getThreadCount() {
		return ((Integer) getConfigurationSetting(CONFIG_TABLE_SETTINGS, CONFIG_TABLE_TIMEOUT)).intValue();
	}

	private int getTimeout() {
		return ((Integer) getConfigurationSetting(CONFIG_TABLE_SETTINGS, CONFIG_TABLE_TIMEOUT)).intValue();
	}
	
	@Override
	public InfoTable processServiceRequest(String serviceName, ValueCollection params) throws Exception {
		return executeService(serviceName, params, false);
	}

	@Override
	public InfoTable processAPIServiceRequest(String serviceName, ValueCollection params) throws Exception {
		return executeService(serviceName, params, true);
	}


	private InfoTable superServiceRequest(String serviceName, ValueCollection params) throws Exception {
		return super.processServiceRequest(serviceName, params);
	}

	private InfoTable superAPIServiceRequest(String serviceName, ValueCollection params) throws Exception {
		return super.processAPIServiceRequest(serviceName, params);
	}

	/*
	 * private InfoTable executeService2(String serviceName, ValueCollection params,
	 * boolean isApi) throws Exception {
	 * 
	 * final ServiceExecutorTemplate thing = this; final SecurityContext
	 * security_context = ThreadLocalContext.getSecurityContext();
	 * 
	 * final Callable<InfoTable> task = () -> { InfoTable result = null;
	 * 
	 * _logger.debug("{}::{} service invoked from ServiceExecutor", thing.getName(),
	 * serviceName); try { ThreadLocalContext.setSecurityContext(security_context);
	 * TransactionFactory.beginTransactionRequired(); if (isApi) { result =
	 * thing.superAPIServiceRequest(serviceName, params); } else { result =
	 * thing.superServiceRequest(serviceName, params); }
	 * ThreadLocalContext.setTransactionSuccess(true); } catch (Exception eExecute)
	 * { try { TransactionFactory.failure(); } catch (Exception e) { //
	 * Intentionally consume this exception }
	 * _logger.debug("{} service execution failed : {}", serviceName,
	 * eExecute.getMessage()); throw eExecute; } finally { try {
	 * TransactionFactory.endTransactionRequired(); } catch (Exception eCommit) {
	 * _logger.error("Unable to commit transaction : {} ", eCommit.getMessage()); }
	 * ThreadLocalContext.cleanupContext(); } return result; };
	 * 
	 * List<Future<InfoTable>> results =
	 * _workerRef.get().invokeAll(Arrays.asList(task), getTimeoutConfig(),
	 * TimeUnit.SECONDS); return results.get(0).get(); }
	 */
	
	private InfoTable executeService(String serviceName, ValueCollection params, boolean isApi) throws Exception {

		final ServiceExecutorTemplate thing = this;
		final SecurityContext security_context = ThreadLocalContext.getSecurityContext();

		final Callable<InfoTable> task = () -> {
			InfoTable result = null;

			_logger.debug("{}::{} service invoked from ServiceExecutor", thing.getName(), serviceName);
			try {
				ThreadLocalContext.setSecurityContext(security_context);
				TransactionFactory.beginTransactionRequired();
				if (isApi) {
					result = thing.superAPIServiceRequest(serviceName, params);
				} else {
					result = thing.superServiceRequest(serviceName, params);
				}
				ThreadLocalContext.setTransactionSuccess(true);
			} catch (Exception eExecute) {
				try {
					TransactionFactory.failure();
				} catch (Exception e) {
					// Intentionally consume this exception
				}
				_logger.debug("{} service execution failed : {}", serviceName, eExecute.getMessage());
				throw eExecute;
			} finally {
				try {
					TransactionFactory.endTransactionRequired();
				} catch (Exception eCommit) {
					_logger.debug("Unable to commit transaction : {} ", eCommit.getMessage());
					ThreadLocalContext.cleanupContext();
					throw eCommit;
				}
				ThreadLocalContext.cleanupContext();
			}
			return result;
		};

		Future<InfoTable> result = _workerRef.get().submit(task);
		try {
			return result.get(getTimeout(), TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			result.cancel(true);
			throw e;
		}
	}

}
