package hwvertx;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.asyncsql.MySQLClient;

public class RestEP extends AbstractVerticle {

	private Map<Integer, PacoState> database;

	private SQLClient mySQLClient;

	public void start(Future<Void> startFuture) {
		database = new HashMap<>();

		JsonObject mySQLClientConfig = new JsonObject().put("host", "127.0.0.1").put("port", 3306)
				.put("database", "paco").put("username", "root").put("password", "4953");

		mySQLClient = MySQLClient.createShared(vertx, mySQLClientConfig);

		Router router = Router.router(vertx);

		vertx.createHttpServer().requestHandler(router::accept).listen(8083, res -> {
			if (res.succeeded()) {
				System.out.println("Servidor REST desplegado");
			} else {
				System.out.println("Error: " + res.cause());
			}
		});

		router.route("/api/elements").handler(BodyHandler.create());
		router.get("/api/elements").handler(this::getAll);
		router.get("/api/elements/:idFilter").handler(this::getOne);
		router.put("/api/elements").handler(this::putElement);
		database.put(1, new PacoState(1, "sensor.puerta", false, 0f));
		database.put(2, new PacoState(2, "sensor.ventana", false, 0f));
		database.put(3, new PacoState(3, "sensor.temperatura", false, 0f));
	}

	private void getOne(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("idFilter");
		if (paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);

				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT id, name, value, state " + "FROM PacoState " + "WHERE id = ?";
						JsonArray paramQuery = new JsonArray().add(param);
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));
							} else {
								routingContext.response().setStatusCode(400).end("Error: " + conn.cause());
							}
						});
					} else {
						routingContext.response().setStatusCode(400).end("Error: " + conn.cause());
					}
				});

				// routingContext.response().setStatusCode(200).end(Json.encodePrettily(database.get(param)));

			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}
	}

	private void getAll(RoutingContext routingContext) {
		routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encode(database.values()));
	}

	private void putElement(RoutingContext routingContext) {
		PacoState state = Json.decodeValue(routingContext.getBodyAsString(), PacoState.class);
		database.put(state.getId(), state);
		routingContext.response().setStatusCode(201).end(Json.encode(state));
	}

}
