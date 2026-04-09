package br.com.gabriel;

import io.javalin.Javalin;
import java.sql.*;
import java.util.ArrayList;
import java.util.Map;

public class App {
    private static final String URL = "jdbc:h2:mem:proj_postman;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASS = "";

    public static void main(String[] args) {
	iniciaBanco();

        // http://localhost:7000/usuarios
        var app = Javalin.create().start(7000);

        // Método Get (get all)
        app.get("/usuarios", request -> {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "SELECT * FROM usuarios";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                var lista = new ArrayList<Map<String, Object>>();
                while (rs.next()) {
                    lista.add(Map.of("id", rs.getInt("id"), "nome", rs.getString("nome"), 
                    "email", rs.getString("email")));
                }
                request.json(lista);
            }
        });

	// Método Get (get by id)
	/* app.get("/usuarios/{id}", request -> {
	    try(Connection conn = DriverManager.getConection(URL, USER, PASS)) {
		String sql = "SELECT * FROM usuarios WHERE id = {id}";
               		PreparedStatement stmt = conn.prepareStatement(sql);
                		ResultSet rs = stmt.executeQuery();
	    } */
	});

        // Método Post
        app.post("/usuarios", request -> {
            var body = request.bodyAsClass(Map.class);
            
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "INSERT INTO usuarios (nome, email) VALUES (?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, body.get("nome").toString());
                stmt.setString(2, body.get("email").toString());
                stmt.executeUpdate();
                
                request.status(201).result("Usuário criado");
            }
        });

        // Método Delete
        app.delete("/usuarios/{id}", request -> {
            int id = Integer.parseInt(request.pathParam("id"));
            
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "DELETE FROM usuarios WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, id);
                int linhasAfetadas = stmt.executeUpdate();

                if (linhasAfetadas > 0) request.result("Usuário removido");
                else request.status(404).result("Usuário não encontrado");
            }
        });
    }
    
    private static void iniciaBanco() {
	try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
		String sql = "CREATE TABLE usuarios (id INT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(100), email VARCHAR(100))";
		conn.createStatement().execute(sql);
		System.out.println("----------------------------------");
		System.out.println("Banco criado na memória");
		System.out.println("----------------------------------");
	} catch (SQLException e) {
		e.printStackTrace();
	}
    }
}
