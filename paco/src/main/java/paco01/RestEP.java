package paco01;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttPublishMessage;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.ext.asyncsql.MySQLClient;



public class RestEP extends AbstractVerticle{
	
	
	private SQLClient mySQLClient;
	private static Multimap<String, MqttEndpoint> topicsCliente;
	////GLOBAL IMPORTANTE
	private String direPuerta;
	public void start(Future<Void> startFuture) {
		
		
		JsonObject mySQLClientConfig = new JsonObject()
				.put("host", "127.0.0.1")
				.put("port", 3306)
				.put("database", "paco")
				.put("username", "root")
				.put("password", "4953")
				;
		
		mySQLClient = MySQLClient.createShared(vertx, mySQLClientConfig);
		Router router = Router.router(vertx);
		
		vertx.createHttpServer().requestHandler(router::accept).
		listen(8083, res -> {
			if (res.succeeded()) {
				System.out.println("Servidor REST desplegado");
				router.getWithRegex(".*(html|css|js|jpg|png|gif)$").handler(url-> {
					String link = url.normalisedPath();
					url.response().sendFile("webroot"+link);
				});
			}else {
				System.out.println("Error: " + res.cause());
			}
		});
		
		router.route("/*").handler(BodyHandler.create());
		router.getWithRegex(".*(html|css|js|jpg|png|gif)$").handler(url-> {
			String link = url.normalisedPath();
			url.response().sendFile("webroot"+link);
		});
		
		router.get("/web/inicio/").handler(this::paginaInicio);
		router.post("/web/inicio/").handler(this::paginaInicio);
		router.post("/pagina/Puerta/").handler(this::logPuerta);
		router.get("/api/door/:id").handler(this::getOnePuerta);
		router.get("/api/doorDir/:dir").handler(this::getPuertabyDir);
		router.get("/api/terminal/:id").handler(this::getOneTerminal);
		router.get("/api/terminal/").handler(this::getTerminal);

		router.delete("/api/door/:id").handler(this::deleteOnePuerta);
		router.delete("/api/terminal/:id").handler(this::deleteOneTerminal);
		
		router.put("/api/door").handler(this::putElementPuerta);
		router.put("/api/terminal").handler(this::putElementTerminal);	
		
		router.get("/api/doorOpen/").handler(this::getMqttAbrirPuerta);
		router.get("/api/doorBlock/").handler(this::getMqttBloquearPuerta);
		router.get("/api/doorUnlock/").handler(this::getMqttDesbloquearPuerta);
		
		router.post("/api/newNormalPass/").handler(this::mqttNuevaPassNormal);
		router.post("/pagina/ContrasenaNormalNueva/").handler(this::cambiaContraN);
		router.post("/api/newMasterPass/").handler(this::mqttNuevaPassMaestra);
		router.post("/pagina/ContrasenaMasterNueva/").handler(this::cambiaContraM);
		router.post("/pagina/cambiarDireccion/").handler(this::cambiaDir);
	
		router.get("/api/mostrarDir/").handler(this::mostrarDir);
		router.get("/api/doorContra/:id").handler(this::contrasenaNormal);
		router.get("/api/doorContraMaestra/:id").handler(this::contrasenaMaestra);

		router.post("/api/door/").handler(this::updateElementPuerta);

		
		
		topicsCliente = HashMultimap.create();

		
		MqttServer mqttServer = MqttServer.create(vertx);
		init(mqttServer);
		MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));
		
		MqttClient mqttClient2 = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));
		mqttClient2.connect(1883, "127.0.0.1", s -> {

			mqttClient2.subscribe("topic_2", MqttQoS.AT_LEAST_ONCE.value(), handler -> {
				if (handler.succeeded() && mqttClient.clientId() != null) {
					
					System.out.println("Cliente " + mqttClient.clientId() + 
							"conexión MQTT correcta.");
					
					mqttClient2.publishHandler(new Handler<MqttPublishMessage>() {
						@Override
						public void handle(MqttPublishMessage arg0) {
							
							System.out.println("Mensaje recibido por el cliente 2: " + arg0.payload().toString());
						}
					});
				}else {
					System.out.println("No hay cliente para suscrbir");
				}
			});
		});		
	}
	
	private static void init(MqttServer mqttServer) {
		mqttServer.endpointHandler(endpoint -> {
			
			System.out.println("Nuevo cliente MQTT [" + endpoint.clientIdentifier()
					+ "] solicitando suscribirse [Id de sesi�n: " + endpoint.isCleanSession() + "]");
			
			endpoint.accept(false);
			handleSubscription(endpoint);
			handleUnsubscription(endpoint);
			publishHandler(endpoint);
			handleClientDisconnect(endpoint);
		}).listen(ar -> {
			if (ar.succeeded()) {
				System.out.println("MQTT server est� a la escucha por el puerto " + ar.result().actualPort());
			} else {
				System.out.println("Error desplegando el MQTT server");
				ar.cause().printStackTrace();
			}
		});
	}
	
	private static void handleSubscription(MqttEndpoint endpoint) {
		endpoint.subscribeHandler(subscribe -> {
			
			List<MqttQoS> grantedQosLevels = new ArrayList<>();
			for (MqttTopicSubscription s : subscribe.topicSubscriptions()) {
				System.out.println("Suscripci�n al topic " + s.topicName() + " con QoS " + s.qualityOfService());
				grantedQosLevels.add(s.qualityOfService());
				
				// A�adimos al cliente en la lista de clientes suscritos al topic
				topicsCliente.put(s.topicName(), endpoint);
			}
		
			
			endpoint.subscribeAcknowledge(subscribe.messageId(), grantedQosLevels);
		});
	}

	
	private static void handleUnsubscription(MqttEndpoint endpoint) {
		endpoint.unsubscribeHandler(unsubscribe -> {
			for (String t : unsubscribe.topics()) {
				
				topicsCliente.remove(t, endpoint);
				System.out.println("Eliminada la suscripci�n del topic " + t);
			}
			// Informamos al cliente que la desuscripci�n se ha realizado
			endpoint.unsubscribeAcknowledge(unsubscribe.messageId());
		});
	}
	
	private static void publishHandler(MqttEndpoint endpoint) {
		endpoint.publishHandler(message -> {
			
			handleMessage(message, endpoint);
		}).publishReleaseHandler(messageId -> {
			
			endpoint.publishComplete(messageId);
		});
	}

	private static void handleMessage(MqttPublishMessage message, MqttEndpoint endpoint) {
		System.out.println("Mensaje publicado por el cliente " + endpoint.clientIdentifier() + " en el topic "
				+ message.topicName());
		System.out.println("    Contenido del mensaje: " + message.payload().toString());
		
		
		System.out.println("Origen: " + endpoint.clientIdentifier());
		for (MqttEndpoint client: topicsCliente.get(message.topicName())) {
			System.out.println("Destino: " + client.clientIdentifier());
			if (!client.clientIdentifier().equals(endpoint.clientIdentifier()))
				client.publish(message.topicName(), message.payload(), message.qosLevel(), message.isDup(), message.isRetain());
		}
		
		if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
			String topicName = message.topicName();
			switch (topicName) {
			}
			endpoint.publishAcknowledge(message.messageId());
		} else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
			
			endpoint.publishRelease(message.messageId());
		}
	}

	
	private static void handleClientDisconnect(MqttEndpoint endpoint) {
		endpoint.disconnectHandler(h -> {
			
			Stream.of(topicsCliente.keySet())
				.filter(e -> topicsCliente.containsEntry(e, endpoint))
				.forEach(s -> topicsCliente.remove(s, endpoint));
			System.out.println("El cliente remoto se ha desconectado [" + endpoint.clientIdentifier() + "]");
		});
	}
	
	//------------__________--------PAGINA INICIO-------------................//
	
	private void paginaInicio(RoutingContext routing) {
		routing.response().sendFile("webroot/inicio.html");
	}
	
	// ------------------------------ ABRIR PUERTA ------------------------------
	
		private void getMqttAbrirPuerta(RoutingContext routingContext) {
			MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));
			mqttClient.connect(1883, "127.0.0.1", s -> {
				
				
				mqttClient.subscribe("topic_2", MqttQoS.AT_LEAST_ONCE.value(), handler -> {
					if (handler.succeeded()) {
						
						System.out.println("Cliente " + mqttClient.clientId() + " suscrito correctamente al canal topic_2");
					}
				});
				mqttClient.publish("topic_2", Buffer.buffer("abrirPuerta"), MqttQoS.AT_LEAST_ONCE, false, false);
				routingContext.response().setStatusCode(200).end();
			});
		}
		
		// ------------------------------ BLOQUEAR PUERTA ------------------------------
		
				private void getMqttBloquearPuerta(RoutingContext routingContext) {
					MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));
					mqttClient.connect(1883, "127.0.0.1", s -> {
						
						
						mqttClient.subscribe("topic_2", MqttQoS.AT_LEAST_ONCE.value(), handler -> {
							if (handler.succeeded()) {
								
								System.out.println("Cliente " + mqttClient.clientId() + " suscrito correctamente al canal topic_2");
							}
						});
						mqttClient.publish("topic_2", Buffer.buffer("blockedDoor"), MqttQoS.AT_LEAST_ONCE, false, false);
						routingContext.response().setStatusCode(200).end();
					});
				}
				
				// ------------------------------ DESBLOQUEAR PUERTA ------------------------------
				
				private void getMqttDesbloquearPuerta(RoutingContext routingContext) {
					MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));
					mqttClient.connect(1883, "127.0.0.1", s -> {
						
						
						mqttClient.subscribe("topic_2", MqttQoS.AT_LEAST_ONCE.value(), handler -> {
							if (handler.succeeded()) {
								
								System.out.println("Cliente " + mqttClient.clientId() + " suscrito correctamente al canal topic_2");
							}
						});
						mqttClient.publish("topic_2", Buffer.buffer("no"), MqttQoS.AT_LEAST_ONCE, false, false);
						routingContext.response().setStatusCode(200).end();
					});
				}
				
						
				
				// ------------------------------ NUEVA PASS NORMAL ------------------------------
				
				private void mqttNuevaPassNormal(RoutingContext routingContext) {
					String paramStr = routingContext.request().getParam("nuevaContra");
					MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));
					System.out.println(paramStr);
					if(paramStr != null) {
						///MQTT
						mqttClient.connect(1883, "127.0.0.1", s -> {						
							mqttClient.subscribe("topic_2", MqttQoS.AT_LEAST_ONCE.value(), handler -> {
								if (handler.succeeded()) {
									System.out.println("Cliente " + mqttClient.clientId() + " suscrito correctamente al canal topic_2");
								}
							});
							String buf = "newNormalPass" + paramStr;
							mqttClient.publish("topic_2", Buffer.buffer(buf), MqttQoS.AT_LEAST_ONCE, false, false);
							routingContext.reroute(HttpMethod.POST, "/pagina/ContrasenaNormalNueva/");
							//routingContext.response().setStatusCode(200).end();
						});
					}
				}
						

				// ------------------------------ NUEVA MASTER PUERTA ------------------------------
				
				private void mqttNuevaPassMaestra(RoutingContext routingContext) {
					String paramStr = routingContext.request().getParam("nuevaMaestra");
					MqttClient mqttClient = MqttClient.create(vertx, new MqttClientOptions().setAutoKeepAlive(true));
					System.out.println(paramStr);
					if(paramStr != null) {
						mqttClient.connect(1883, "127.0.0.1", s -> {						
							mqttClient.subscribe("topic_2", MqttQoS.AT_LEAST_ONCE.value(), handler -> {
								if (handler.succeeded()) {
								
									System.out.println("Cliente " + mqttClient.clientId() + " suscrito correctamente al canal topic_2");
								}
							});
							String buf = "newMasterPass" + paramStr;
							mqttClient.publish("topic_2", Buffer.buffer(buf), MqttQoS.AT_LEAST_ONCE, false, false);
							routingContext.reroute(HttpMethod.POST, "/pagina/ContrasenaMasterNueva/");
							//routingContext.response().setStatusCode(200).end();
						});
					}
				}





	// ------------------------------ GETONEs ------------------------------
	
	
	private void getOnePuerta(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("id");
		if(paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT idPuerta, estadoPuerta, direccionPuerta, contraPuerta, adminPuerta "
						+ "FROM puerta "
						+ "WHERE idPuerta = ?";
						
						JsonArray paramQuery = new JsonArray()
								.add(param);
						
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));
							}else {
								routingContext.response().setStatusCode(400).end(res.cause().toString());
							}
						});
						
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}else {
			routingContext.response().setStatusCode(400).end();
		}
	}
	//-----------------------------------------LOGIN--------------------------------
	private void logPuerta(RoutingContext routingContext) {
		String direccion = routingContext.request().getParam("dir");
		String contrasena = routingContext.request().getParam("con");
		if(direccion != null) {
			try {
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT adminPuerta "
						+ "FROM puerta "
						+ "WHERE direccionPuerta = ?";
						
						JsonArray paramQuery = new JsonArray()
								.add(direccion);
						
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								String getContra = (String) res.result().getRows().get(0).getValue("adminPuerta");
								if (getContra.equals(contrasena)) {
									///////////////////////////redirigir
									direPuerta = direccion;
									routingContext.response().sendFile("webroot/paginaUsuario.html");
								}else {
									routingContext.response().sendFile("webroot/error_acceso.html");
								}
							}else {
								routingContext.response().sendFile("webroot/error_acceso.html");
							}
						});
					
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}else {
			routingContext.response().setStatusCode(400).end();
		}
	}
	//------------------------- ACTUALIZA CONTRA ARDUINO
	private void contrasenaNormal(RoutingContext routingContext) {
		String dato = routingContext.request().getParam("id");
		if(dato != null) {
			try {
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT contraPuerta "
						+ "FROM puerta "
						+ "WHERE idPuerta = ?";
						
						JsonArray paramQuery = new JsonArray()
								.add(dato);
						
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								String getContra = (String) res.result().getRows().get(0).getValue("contraPuerta");
								routingContext.response().end(getContra);
							}else {
								routingContext.response().setStatusCode(400).end(res.cause().toString());
							}
						});
						
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}
	}
	
	//------------------------- ACTUALIZA CONTRA MAESTRA ARDUINO
		private void contrasenaMaestra(RoutingContext routingContext) {
			String dato = routingContext.request().getParam("id");
			if(dato != null) {
				try {
					mySQLClient.getConnection(conn -> {
						if(conn.succeeded()) {
							SQLConnection connection = conn.result();
							String query = "SELECT adminPuerta "
							+ "FROM puerta "
							+ "WHERE idPuerta = ?";
							
							JsonArray paramQuery = new JsonArray()
									.add(dato);
							
							connection.queryWithParams(query, paramQuery, res -> {
								if (res.succeeded()) {
									String getContra = (String) res.result().getRows().get(0).getValue("adminPuerta");
									routingContext.response().end(getContra);
								}else {
									routingContext.response().setStatusCode(400).end(res.cause().toString());
								}
							});
						}else {
							routingContext.response().setStatusCode(400).end(conn.cause().toString());
						}
					});
				}catch (ClassCastException e) {
					routingContext.response().setStatusCode(400).end();
				}
			}
		}
	
	//-------------------MOSTRAR PUERTA
	private void mostrarDir(RoutingContext routingContext) {	
		System.out.println(direPuerta);
		routingContext.response().end(direPuerta);
	}
//----------------------------------------------------------------------------------------
	private void getPuertabyDir(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("dir");
		if(paramStr != null) {
			try {
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT idPuerta, estadoPuerta, direccionPuerta, contraPuerta, adminPuerta "
						+ "FROM puerta "
						+ "WHERE direccionPuerta = ?";
						
						JsonArray paramQuery = new JsonArray()
								.add(paramStr);
						
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));
							}else {
								routingContext.response().setStatusCode(400).end(res.cause().toString());
							}
						});
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}else {
			routingContext.response().setStatusCode(400).end();
		}
	}
	
	
	
	private void getOneTerminal(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("id");
		if(paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT idTerminal, acierto, fecha, bloqueo, numIntento, puertaRelacionada "
						+ "FROM terminal "
						+ "WHERE idTerminal = ?";
						JsonArray paramQuery = new JsonArray()
								.add(param);
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));
							}else {
								routingContext.response().setStatusCode(400).end(res.cause().toString());
							}
						});
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}else {
			routingContext.response().setStatusCode(400).end();
		}
	}
	
	private void getTerminal(RoutingContext routingContext) {
			try {
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT acierto, fecha, bloqueo, numIntento" + 
								" FROM terminal" + 
								" INNER JOIN puerta ON terminal.puertaRelacionada = puerta.idPuerta" + 
								" WHERE puerta.direccionPuerta = ? ";
						JsonArray paramQuery = new JsonArray()
								.add(direPuerta);
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));
							}else {
								routingContext.response().setStatusCode(400).end(res.cause().toString());
							}
						});
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
	}
	
	
	
	
	// ------------------------------ DELETEs ------------------------------
	
	private void deleteOneTerminal(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("id");
		if(paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "DELETE FROM terminal "
						+ "WHERE idTerminal = ?";
						JsonArray paramQuery = new JsonArray()
								.add(param);
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));
							}else {
								routingContext.response().setStatusCode(400).end(res.cause().toString());
							}
						});
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}else {
			routingContext.response().setStatusCode(400).end();
		}
	}
	
	
	
	private void deleteOnePuerta(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("id");
		if(paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "DELETE FROM puerta "
						+ "WHERE idPuerta = ?";
						JsonArray paramQuery = new JsonArray()
								.add(param);
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end("Successfully deleted.");
							}else {
								routingContext.response().setStatusCode(400).end(res.cause().toString());
							}
						});
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}else {
			routingContext.response().setStatusCode(400).end();
		}
	}
	
	
	
	
	
	
	
	
	
	// ------------------------------ PUTs ------------------------------
	

	
	private void putElementPuerta(RoutingContext routingContext) {
		Puerta state = Json.decodeValue(routingContext.getBody().toString(), Puerta.class);		
			try {
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "INSERT INTO Puerta (idPuerta, estadoPuerta, direccionPuerta, contraPuerta, adminPuerta) "
						+ "VALUES (?, ?, ?, ?, ?) ";
						JsonArray paramQuery = new JsonArray()
								.add(state.getId())
								.add(state.getDoorState())
								.add(state.getDoorAddress())
								.add(state.getDoorPass())
								.add(state.getDoorAdmin());
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(state));
							}else {
								routingContext.response().setStatusCode(400).end(res.cause().toString());
							}
						});	
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
	}
	
	
	
	private void putElementTerminal(RoutingContext routingContext) {
		Terminal state = Json.decodeValue(routingContext.getBody().toString(), Terminal.class);
		Date date = new Date();
			try {
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "INSERT INTO Terminal (idTerminal, acierto, fecha, bloqueo, numIntento, puertaRelacionada) "
						+ "VALUES (?, ?, ?, ?, ?, ?) ";
						JsonArray paramQuery = new JsonArray()
								.add(state.getId())
								.add(state.isTryState())
								.add(date.toString())
								.add(state.isTryBlock())
								.add(state.getTryNumber())
								.add(state.getRelatedDoor());
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(state));
							}else {
								routingContext.response().setStatusCode(400).end(res.cause().toString());
							}
						});
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
	}
	
	//---------------------------------UPDATE GENERAL
	private void updateElementPuerta(RoutingContext routingContext) {
		Puerta state = Json.decodeValue(routingContext.getBody().toString(), Puerta.class);		
			try {
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "UPDATE Puerta "
						+ "SET estadoPuerta=?, contraPuerta=?, adminPuerta=? "
						+ "WHERE idPuerta = ?";
						JsonArray paramQuery = new JsonArray()
								.add(state.getDoorState())
								.add(state.getDoorPass())
								.add(state.getDoorAdmin())
								.add(state.getId());
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(state));
							}else {
								routingContext.response().setStatusCode(400).end(res.cause().toString());
							}
						});	
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
	}
	//---------------------UPDATE CONTRANORMAL BD

	private void cambiaContraN(RoutingContext routingContext) {
		String actualizar = routingContext.request().getParam("nuevaContra");

			try {
				mySQLClient.getConnection(conn -> {
					if(conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "UPDATE Puerta "
						+ "SET contraPuerta=?"
						+ " WHERE direccionPuerta = ?";
						JsonArray paramQuery = new JsonArray()
								.add(actualizar)
								.add(direPuerta);
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.reroute(HttpMethod.GET, "web/inicio/");
							}else {
								routingContext.response().setStatusCode(400).end(res.cause().toString());
							}
						});	
					}else {
						routingContext.response().setStatusCode(400).end(conn.cause().toString());
					}
				});
			}catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
	}
	
	//---------------------UPDATE CONTRAMASTER BD

		private void cambiaContraM(RoutingContext routingContext) {
			String actualizar = routingContext.request().getParam("nuevaMaestra");

				try {
					mySQLClient.getConnection(conn -> {
						if(conn.succeeded()) {
							SQLConnection connection = conn.result();
							String query = "UPDATE Puerta "
							+ "SET adminPuerta=?"
							+ " WHERE direccionPuerta = ?";
							JsonArray paramQuery = new JsonArray()
									.add(actualizar)
									.add(direPuerta);
							connection.queryWithParams(query, paramQuery, res -> {
								if (res.succeeded()) {
									routingContext.reroute(HttpMethod.GET, "web/inicio/");
								}else {
									routingContext.response().setStatusCode(400).end(res.cause().toString());
								}
							});	
						}else {
							routingContext.response().setStatusCode(400).end(conn.cause().toString());
						}
					});
				}catch (ClassCastException e) {
					routingContext.response().setStatusCode(400).end();
				}
		}
//-------------------------------------CAMBIA DIRECCION----------------//
		private void cambiaDir(RoutingContext routingContext) {
			String actualizar = routingContext.request().getParam("nuevaDir");

				try {
					mySQLClient.getConnection(conn -> {
						if(conn.succeeded()) {
							SQLConnection connection = conn.result();
							String query = "UPDATE Puerta "
							+ "SET direccionPuerta=?"
							+ " WHERE direccionPuerta = ?";
							JsonArray paramQuery = new JsonArray()
									.add(actualizar)
									.add(direPuerta);
							connection.queryWithParams(query, paramQuery, res -> {
								if (res.succeeded()) {
									routingContext.reroute(HttpMethod.GET, "web/inicio/");
								}else {
									routingContext.response().setStatusCode(400).end(res.cause().toString());
								}
							});	
						}else {
							routingContext.response().setStatusCode(400).end(conn.cause().toString());
						}
					});
				}catch (ClassCastException e) {
					routingContext.response().setStatusCode(400).end();
				}
		}

	
}
