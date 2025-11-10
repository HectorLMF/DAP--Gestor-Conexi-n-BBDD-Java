package org.example.db;

import java.util.List;
import java.util.Map;

/**
 * @file DBClient.java
 * @brief Cliente genérico que actúa como fachada (middleware) sobre las
 *        fábricas y conexiones de base de datos.
 *
 * DBClient encapsula la lógica común para obtener una conexión a partir de
 * una {@link DBFactory}, abrirla, ejecutar consultas en modo texto y cerrar
 * la conexión. Su propósito es proporcionar una API simple y estable a la
 * capa web (servidor HTTP) sin exponer detalles concretos del proveedor.
 *
 * Contrato / comportamiento:
 * - Construcción: DBClient(factory, connName)
 * - connect(): crea la conexión con factory.createConnection(connName) y la abre
 * - executeText(sql): crea una {@link DBQuery} con factory.createQuery(conn)
 *   y delega la ejecución. Lanza IllegalStateException si no se llamó a connect()
 * - disconnect(): cierra la conexión si está abierta
 *
 * Errores y excepciones:
 * - Si la fábrica/implementación subyacente falla al conectar, se propaga una
 *   RuntimeException con el detalle para que el servidor web lo convierta en
 *   una respuesta HTTP 5xx.
 *
 * Uso típico (en servidor web):
 * DBFactory factory = new PostgressFactory();
 * DBClient client = new DBClient(factory, "web-demo");
 * client.connect();
 * List<Map<String,Object>> rows = client.executeText("SELECT ...");
 * client.disconnect();
 *
 * @author Equipo
 */
public class DBClient {
  private final DBFactory factory;
  private DBConnection conn;
  private final String connName;

  /**
   * Constructor.
   *
   * @param factory fábrica concreta para el proveedor deseado
   * @param connName nombre lógico de la conexión (puede usarse para distinguir
   *                 múltiples conexiones en un mismo proveedor)
   */
  public DBClient(DBFactory factory, String connName) {
    this.factory = factory;
    this.connName = connName;
  }

  /**
   * Crea la conexión a través de la fábrica y la abre.
   *
   * Lanza RuntimeException si la creación o apertura falla.
   */
  public void connect() {
    this.conn = factory.createConnection(connName);
    this.conn.connect();
  }

  /**
   * Cierra la conexión si existe. Seguro de llamar varias veces.
   */
  public void disconnect() {
    if (this.conn != null) this.conn.disconnect();
  }

  /**
   * Ejecuta una consulta SQL en modo texto y devuelve las filas (columna->valor).
   *
   * Este método es la forma principal en que el servidor web pide al middleware
   * que ejecute SQL sin conocer el proveedor.
   *
   * @param sql sentencia SQL en texto
   * @return lista de filas (cada fila: Map nombreColumna->valor)
   * @throws IllegalStateException si no se ha llamado a connect() previamente
   */
  public List<Map<String,Object>> executeText(String sql) {
    if (conn == null) throw new IllegalStateException("No connection. Call connect() first.");
    DBQuery q = factory.createQuery(conn);
    q.setSql(sql);
    return q.execute();
  }

  /**
   * Obtiene la conexión subyacente (útil para inspección/logging en demos).
   *
   * @return la instancia de {@link DBConnection} o null si no se creó aún
   */
  public DBConnection getConnection() { return conn; }
}