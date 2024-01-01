package lk.ijse.dep11.todo.api;

import com.fasterxml.jackson.databind.ser.std.StdKeySerializers;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lk.ijse.dep11.todo.to.TaskTO;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PreDestroy;
import javax.validation.Valid;
import javax.validation.groups.Default;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tasks")
//@CrossOrigin("http://localhost:1234")
@CrossOrigin
public class TaskHttpController {

    private final HikariDataSource pool;

    public TaskHttpController(){
        HikariConfig config = new HikariConfig();
        config.setUsername("postgres");
        config.setPassword("961021");
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/dep11_todo_app");
        config.setDriverClassName("org.postgresql.Driver");
        config.addDataSourceProperty("maximumPoolSize",10);
        pool = new HikariDataSource(config);
    }

    @PreDestroy
    public void destroy(){
        pool.close();
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(produces = "application/json", consumes = "application/json")
    public TaskTO createTask(@RequestBody @Validated(TaskTO.Create.class) TaskTO task){
        try (
            Connection connection = pool.getConnection()){
            PreparedStatement pst = connection.prepareStatement("INSERT INTO task (description, status,email) VALUES (?,FALSE,?)", Statement.RETURN_GENERATED_KEYS);
            pst.setString(1,task.getDescription());
            pst.setString(2,task.getEmail());
            pst.executeUpdate();
            ResultSet generatedKeys = pst.getGeneratedKeys();
            generatedKeys.next();
            int id = generatedKeys.getInt(1);
            task.setId(id);
            task.setStatus(false);
            return task;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // PATCH /tasks/{id}
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping(value = "/{id}", consumes = "application/json")
    public void updateTask(@PathVariable("id") int taskId,
            @RequestBody @Validated(TaskTO.Update.class) TaskTO task){
        try (Connection connection = pool.getConnection()) {
            PreparedStatement stmExist = connection.prepareStatement("SELECT * FROM task WHERE id = ?");
            stmExist.setInt(1,taskId);
            if (!stmExist.executeQuery().next()){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Task Not Found");
            }

            PreparedStatement stm = connection.prepareStatement("UPDATE task SET description = ?, status = ? WHERE id=?");
            stm.setString(1,task.getDescription());
            stm.setBoolean(2,task.getStatus());
            stm.setInt(3,taskId);
            stm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // DELETE /tasks/{id}
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable("id") int taskId){
        try(Connection connection = pool.getConnection()){
            PreparedStatement stmExist = connection
                    .prepareStatement("SELECT * FROM task WHERE id = ?");
            stmExist.setInt(1, taskId);
            if (!stmExist.executeQuery().next()){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task Not Found");
            }

            PreparedStatement stm = connection.prepareStatement("DELETE FROM task WHERE id=?");
            stm.setInt(1, taskId);
            stm.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping(produces = "application/json")
    public List<TaskTO> getAllTasks(String email){
        try (Connection connection = pool.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM task WHERE email =? ORDER BY id");
            preparedStatement.setString(1,email);
            ResultSet rst = preparedStatement.executeQuery();
            List<TaskTO> taskList = new LinkedList<>();
            while (rst.next()){
                int id = rst.getInt("id");
                boolean status = rst.getBoolean("status");
                String description = rst.getString("description");
                taskList.add(new TaskTO(id,description,status,email));
            }
            return taskList;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
