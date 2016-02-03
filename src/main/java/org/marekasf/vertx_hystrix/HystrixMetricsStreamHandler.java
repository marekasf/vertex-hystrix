package org.marekasf.vertx_hystrix;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller;

import rx.schedulers.Schedulers;

public class HystrixMetricsStreamHandler
{
	private static final Logger log = LoggerFactory.getLogger(HystrixMetricsStreamHandler.class);

	private static final AtomicInteger concurrentConnections = new AtomicInteger(0);
	private static final DynamicIntProperty maxConcurrentConnections = DynamicPropertyFactory.getInstance().getIntProperty(
			"hystrix.stream.maxConcurrentConnections", 5);

	public void hystrixStream(final HttpServerRequest vertxRequest)
	{

		/* ensure we aren't allowing more connections than we want */
		int numberConnections = concurrentConnections.incrementAndGet();

		if (numberConnections > maxConcurrentConnections.get())
		{
			vertxRequest.response().setStatusCode(503) //
					.setStatusMessage("MaxConcurrentConnections reached: " + maxConcurrentConnections.get()).end();
		}
		else
		{
			final MetricJsonListener jsonListener = new MetricJsonListener();

			vertxRequest.response().setChunked(true);
			vertxRequest.exceptionHandler(event -> {
				log.error("Error in hystrix.stream request", event.getCause());
				jsonListener.handle(null);
			});
			vertxRequest.response().closeHandler(jsonListener);

			rx.util.async.Async.runAsync(Schedulers.io(), (observer, subscription) -> {
				jsonListener.streamMetrics(vertxRequest);
				observer.onCompleted();
			}).subscribe();
		}
	}

	/**
	 * This will be called from another thread so needs to be thread-safe.
	 */
	private static class MetricJsonListener implements HystrixMetricsPoller.MetricsAsJsonPollerListener, Handler<Void>
	{

		private volatile boolean isDestroyed = false;

		/**
		 * Setting limit to 1000. In a healthy system there isn't any reason to hit this limit so if we do it will throw an
		 * exception which causes the poller to stop.
		 * <p>
		 * This is a safety check against a runaway poller causing memory leaks.
		 */
		private final LinkedBlockingQueue<String> jsonMetrics = new LinkedBlockingQueue<>(1000);

		/**
		 * Store JSON messages in a queue.
		 */
		@Override
		public void handleJsonMetric(String json)
		{
			jsonMetrics.add(json);
		}

		/**
		 * Get all JSON messages in the queue.
		 *
		 * @return metrics
		 */
		public List<String> getJsonMetrics()
		{
			ArrayList<String> metrics = new ArrayList<>();
			jsonMetrics.drainTo(metrics);
			return metrics;
		}

		private void streamMetrics(final HttpServerRequest vertxRequest)
		{
			HystrixMetricsPoller poller = null;
			try
			{
				int delay = 1000;
				try
				{
					String d = vertxRequest.params().get("delay");
					if (d != null)
					{
						delay = Integer.parseInt(d);
					}
				}
				catch (Exception e)
				{
					// ignore if it's not a number
				}

				/* initialize response */
				vertxRequest.response().headers().set("Content-Type", "text/event-stream;charset=UTF-8");
				vertxRequest.response().headers().set("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
				vertxRequest.response().headers().set("Pragma", "no-cache");

				poller = new HystrixMetricsPoller(this, delay);
				// start polling and it will write directly to the output stream
				poller.start();
				log.info("Starting poller");

				// we will use a "single-writer" approach where the Servlet thread does all the writing
				// by fetching JSON messages from the MetricJsonListener to write them to the output
				try
				{
					while (poller.isRunning() && !isDestroyed)
					{
						List<String> jsonMessages = this.getJsonMetrics();
						if (jsonMessages.isEmpty())
						{
							vertxRequest.response().write("event: ping\n");
						}
						else
						{
							for (String json : jsonMessages)
							{
								vertxRequest.response().write("data: " + json + "\n\n");
							}
						}

						/* shortcut breaking out of loop if we have been destroyed */
						if (isDestroyed)
						{
							break;
						}

						// now wait the 'delay' time
						Thread.sleep(delay);
					}
				}
				catch (Exception e)
				{
					poller.shutdown();
					log.error("Failed to write. Will stop polling.", e);
				}
				log.debug("Stopping Turbine stream to connection");
			}
			catch (Exception e)
			{
				log.error("Error initializing servlet for metrics event stream.", e);
			}
			finally
			{
				concurrentConnections.decrementAndGet();
				if (poller != null)
				{
					poller.shutdown();
				}
			}
		}

		@Override
		public void handle(final Void event)
		{
			isDestroyed = true;
		}
	}
}
