package hwvertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;

public class dadDos extends AbstractVerticle {
	public void start(Future<Void> startFuture) throws Exception {
		System.out.println("Me he desplegado bien");
		startFuture.succeeded();
		startFuture.complete();

		vertx.eventBus().consumer("mensaje-p2p", msg -> {
			System.out.println(msg.body().toString());
			msg.reply("Sí, kbsa", resp -> {
				System.out.println((resp.result().body()));
			});
		});

		vertx.eventBus().consumer("mensaje-Broadcast", msg -> {
			System.out.println(msg.body().toString());
		});
	}
}
