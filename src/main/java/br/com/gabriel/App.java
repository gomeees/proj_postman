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
            try(Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "SELECT * FROM usuarios";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                var lista = new ArrayList<Map<String, Object>>();
                while(rs.next()) {
                    lista.add(Map.of("id", rs.getInt("id"), "nome", rs.getString("nome"), "email", rs.getString("email")));
                }
                request.json(lista);
            }
        });

	// Método Get (get by id)
	app.get("/usuarios/{id}", request -> {
	    int id = Integer.parseInt(request.pathParam("id"));
	
	    try(Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
		String sql = "SELECT * FROM usuarios WHERE id = ?";
               	PreparedStatement stmt = conn.prepareStatement(sql);
               	stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                
                if(rs.next()) {
                     var usuario = Map.of("id", rs.getInt("id"), "nome", rs.getString("nome"), "email", rs.getString("email"));
                     request.json(usuario);
                } else request.status(404).result("ID de usuário não encontrado");
	    }
	}); 

        // Método Post
        app.post("/usuarios", request -> {
            var body = request.bodyAsClass(Map.class);
            String novoNome = body.get("nome").toString();
            String novoEmail = body.get("email").toString();
            
            try(Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            	String emailJaExiste = "SELECT id FROM usuarios WHERE email = ?";
            	PreparedStatement stmtVerificacao = conn.prepareStatement(emailJaExiste);
            	stmtVerificacao.setString(1, novoEmail);
            	ResultSet rs = stmtVerificacao.executeQuery();
            	
            	if(rs.next()) {
            	     request.status(409).result("Erro: o e-mail " + novoEmail + " já está em uso");
            	     return;
            	}
            	
                String sql = "INSERT INTO usuarios (nome, email) VALUES (?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, novoNome);
                stmt.setString(2, novoEmail);
                stmt.executeUpdate();
                
                request.status(201).result("Usuário criado");
            }
        });
	
	// Método Put
	app.put("/usuarios/{id}", request -> {
	    int id = Integer.parseInt(request.pathParam("id"));
	    var body = request.bodyAsClass(Map.class);
	    String novoNome = body.get("nome").toString();
	    String novoEmail = body.get("email").toString();
	    
	    try(Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
	    	String sql = "UPDATE usuarios SET nome = ?, email = ? WHERE id = ?";
	    	PreparedStatement stmt = conn.prepareStatement(sql);
	    	stmt.setString(1, novoNome);
	    	stmt.setString(2, novoEmail);
	    	stmt.setInt(3, id);
	    	
	    	int linhasAfetadas = stmt.executeUpdate();
	    	
	    	if(linhasAfetadas > 0) request.result("Usuário atualizado");
	    	else request.status(404).result("Usuário não encontrado para atualizar");
	    }
	});
	
	// Método Patch
	app.patch("/usuarios/{id}", request -> {
	    int id = Integer.parseInt(request.pathParam("id"));
	    var body = request.bodyAsClass(Map.class);
	    
	    if(body.isEmpty()) {
	    	request.status(400).result("Nenhum dado encontrado");
	    	return;
	    }
	    
	    StringBuilder sql = new StringBuilder("UPDATE usuarios SET ");
	    var valores = new ArrayList<Object>();
	    
	    body.forEach((chave, valor) -> {
	    	sql.append(chave).append(" = ?, ");
	    	valores.add(valor);
	    });
	    
	    sql.delete(sql.length() - 2, sql.length());
	    sql.append(" WHERE id = ?");
	    valores.add(id);
	    
	    try(Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
	    	PreparedStatement stmt = conn.prepareStatement(sql.toString());
	    	
	    	for(int i = 0; i < valores.size(); i++) {
	    	    stmt.setObject(i + 1, valores.get(i));
	    	}
	    	
	    	int atualizados = stmt.executeUpdate();
	    	
	    	if(atualizados > 0) request.result("Campos atualizados com sucesso");
	    	else request.status(404).result("Usuário não encontrado");
	    }
	});
	
        // Método Delete
        app.delete("/usuarios/{id}", request -> {
       	    String token = request.header("chave");
       	    String admin = "admin123";
            
            if(token == null || !token.equals(admin)) {
            	request.status(401).result("Não autorizado: Chave de acesso inválida");
            	return;
            }
            
            int id = Integer.parseInt(request.pathParam("id"));
            
            try(Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                String sql = "DELETE FROM usuarios WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, id);
                int linhasAfetadas = stmt.executeUpdate();

                if(linhasAfetadas > 0) request.status(204);
                else request.status(404).result("Usuário não encontrado para remover");
            }
        });
        
        // Captura erros gerais no banco (substitui os catch nos métodos)
	app.exception(SQLException.class, (e, request) -> {
	    System.err.println("Erro de SQL: " + e.getMessage());
	    request.status(500).result("Erro interno no banco de dados");
	});
    }
    
    private static void iniciaBanco() {
	try(Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
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
