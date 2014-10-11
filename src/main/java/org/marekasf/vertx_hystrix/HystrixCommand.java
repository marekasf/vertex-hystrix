package org.marekasf.vertx_hystrix;

import org.vertx.java.core.logging.Logger;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;

import rx.Observable;
import rx.functions.Func0;

class HystrixCommand<T> extends HystrixObservableCommand<T>
{
	private final Func0<Observable<T>> func;
	private final Logger logger;

	public HystrixCommand(final String name, final String group, final int timeoutMs,
			final org.vertx.java.core.logging.Logger logger, final Func0<Observable<T>> func)
	{
		super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(group)) //
				.andCommandKey(HystrixCommandKey.Factory.asKey(name)) //
				.andCommandPropertiesDefaults(HystrixCommandProperties.Setter() //
						.withExecutionIsolationThreadTimeoutInMilliseconds(timeoutMs)));

		if (logger.isDebugEnabled())
		{
			logger.debug("Created command " + this.getCommandKey().name() + ":" + this.getCommandGroup().name());
		}

		this.func = func;
		this.logger = logger;
	}

	@Override
	protected Observable<T> run()
	{
		if (logger.isDebugEnabled())
		{
			logger.info("Running command " + this.getCommandKey().name() + ":" + this.getCommandGroup().name());
		}
		return func.call();
	}
}
