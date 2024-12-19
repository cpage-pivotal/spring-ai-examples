package org.springframework.ai.mcp.samples.jetbrains;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.mcp.client.McpClient;
import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.mcp.client.stdio.ServerParameters;
import org.springframework.ai.mcp.client.stdio.StdioServerTransport;
import org.springframework.ai.mcp.spring.McpFunctionCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner interactiveChat(ChatClient.Builder chatClientBuilder,
                                             List<McpFunctionCallback> functionCallbacks,
                                             ConfigurableApplicationContext context) {

        return args -> {

            var chatClient = chatClientBuilder
                    .defaultFunctions(functionCallbacks.toArray(new McpFunctionCallback[0]))
                    .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                    .build();

            var scanner = new Scanner(System.in);
            System.out.println("\nStarting interactive chat session. Type 'exit' to quit.");

            try {
                while (true) {
                    System.out.print("\nUSER: ");
                    String input = scanner.nextLine();

                    if (input.equalsIgnoreCase("exit")) {
                        System.out.println("Ending chat session.");
                        break;
                    }

                    System.out.print("ASSISTANT: ");
                    System.out.println(chatClient.prompt(input).call().content());
                }
            } finally {
                scanner.close();
                context.close();
            }
        };
    }

    @Bean
    public List<McpFunctionCallback> functionCallbacks(McpSyncClient mcpClient) {

        var callbacks = mcpClient.listTools(null)
                .tools()
                .stream()
                .map(tool -> new McpFunctionCallback(mcpClient, tool))
                .toList();
        return callbacks;
    }

    @Bean(destroyMethod = "close")
    public McpSyncClient mcpClient() {

        var stdioParams = ServerParameters.builder("npx")
                .args("-y", "@jetbrains/mcp-proxy")
                .build();

        var mcpClient = McpClient.sync(new StdioServerTransport(stdioParams));
        var init = mcpClient.initialize();
        System.out.println("MCP Initialized: " + init);
        return mcpClient;
    }

}
