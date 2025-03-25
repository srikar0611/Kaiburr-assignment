package com.example.demo.services;

import com.example.demo.models.Task;
import com.example.demo.models.TaskExecution;
import com.example.demo.repositories.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository;

    // Allowed commands for security
    private static final String[] SAFE_COMMANDS = {"echo", "ls", "date", "uptime"};

    // Regex to detect dangerous shell characters
    private static final Pattern UNSAFE_PATTERN = Pattern.compile("[;&|><`$()]");

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    public Task createTask(Task task) {
        if (!isValidCommand(task.getCommand())) {
            throw new IllegalArgumentException("Command is not allowed for security reasons.");
        }
        return taskRepository.save(task);
    }

    public boolean deleteTask(String id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Task> findTasksByName(String name) {
        return taskRepository.findByNameContaining(name);
    }

    // Validate command security
    private boolean isValidCommand(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }
        if (UNSAFE_PATTERN.matcher(command).find()) {
            return false;
        }
        for (String safe : SAFE_COMMANDS) {
            if (command.startsWith(safe)) {
                return true;
            }
        }
        return false;
    }

    public String executeTask(String id) {
        Optional<Task> taskOptional = taskRepository.findById(id);
        if (taskOptional.isEmpty()) {
            return "Task not found!";
        }

        Task task = taskOptional.get();
        String command = task.getCommand();

        if (!isValidCommand(command)) {
            return "Command is not allowed!";
        }

        Instant startTime = Instant.now();
        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder processBuilder;
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            } else {
                processBuilder = new ProcessBuilder("sh", "-c", command);
            }

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            process.waitFor();
        } catch (Exception e) {
            return "Error executing task: " + e.getMessage();
        }

        Instant endTime = Instant.now();

        // Store execution history
        TaskExecution execution = new TaskExecution(startTime, endTime, output.toString().trim());
        task.getTaskExecutions().add(execution);
        taskRepository.save(task);

        return output.toString().trim();
    }
}
