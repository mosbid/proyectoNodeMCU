package hwvertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class myFirstApp extends AbstractVerticle {
	private HttpServer server;

	public void start() throws Exception {
		super.start();

		JsonObject json = new JsonObject();
		json.put("numero", 18);
		json.put("plazas", 32);
		json.put("nombre", "AV74");

		JsonObject plaza = new JsonObject();
		plaza.put("posicion", "3A");
		plaza.put("emergencia", true);
		plaza.put("plaza", plaza);

		json.put("hora", 18);

		System.out.println(json.encodePrettily());

		String jsonString = json.encode();
		JsonObject jsonResult = new JsonObject(jsonString);

		System.out.println(jsonResult.getInteger("numero"));
		System.out.println(jsonResult.getJsonObject("plaza"));

		lectura lectura = new lectura();
		lectura.setHumedad(89);
		lectura.setTemperatura(20);
		Json.encode(lectura);

		String lecturaStr = Json.encode(lectura);
		System.out.println(lecturaStr);

		lectura l = Json.decodeValue(lecturaStr, lectura.class);
		System.out.println(l.toString());

		vertx.executeBlocking(param -> {
			try {
				Thread.sleep(10000);
				param.complete("Finalizado");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}, param2 -> {
			System.out.println(param2.result().toString());
		});

		vertx.deployVerticle(dadDos.class.getName(), result -> {
			if (result.succeeded()) {
				System.out.println("Veticle dadDos desplegado");
			} else {
				System.out.println("Error al desplegar el verticle");
				result.cause().getStackTrace();
			}

		});
		server = vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {
			public void handle(HttpServerRequest req) {
				req.response().end("Hola mundo");
				// server.close();
			}
		}).listen(8081);

		EventBus bus = vertx.eventBus();
		vertx.setPeriodic(4000, handler -> {
			bus.send("mensaje-p2p", "Hola soy el primero de todos, ¿estás?", res -> {
				System.out.println(res.result().body());
				res.result().reply("OCpuntoespacio");
			});
		});

		vertx.setPeriodic(1000, handler -> {
			bus.publish("mensaje-Broadcast", "Mensaje para todos");
		});

	}

	public void stop() {
		if (server != null)
			server.close();
	}
	/*
	 * public void start(Future<Void> startFuture) throws Exception {
	 * super.start(startFuture); vertx .createHttpServer() .requestHandler(r -> {
	 * r.response().end("<h1>Bienvenido a mi primera aplicacion Vert.x 3</h1>" +
	 * "Esto es un ejemplo de una Verticle sencillo para probar el despliegue"); })
	 * .listen(8081, result -> { if (result.succeeded()) { startFuture.complete(); }
	 * else { startFuture.fail(result.cause()); } }); }
	 */

}
