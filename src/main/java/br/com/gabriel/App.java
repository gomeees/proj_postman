package br.com.gabriel;

import io.javalin.Javalin;
import java.sql.*;
import java.util.ArrayList;
import java.util.Map;

public class App {
    private static final String URL = "jdbc:mysql://localhost:3306/proj_postman";
    private static final String USER = "root";
    private static final String PASS = "admin123";

    public static void main(String[] args) {
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
            } catch (SQLException e) {
                request.status(500).result("Erro: " + e.getMessage());
            }
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
            } catch (SQLException e) {
                request.status(500).result("Erro: " + e.getMessage());
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
}
