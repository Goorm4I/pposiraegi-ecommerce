package cloud.pposiraegi.gateway.monitoring;

import cloud.pposiraegi.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private static final String NODE_CPU_QUERY =
            "100 * (1 - avg by (instance) (rate(node_cpu_seconds_total{mode=\"idle\"}[2m])))";
    private static final String NODE_MEMORY_QUERY =
            "100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))";

    private final WebClient prometheusClient;

    public MonitoringController(
            WebClient.Builder webClientBuilder,
            @Value("${monitoring.prometheus.base-url:http://localhost:9090}") String prometheusBaseUrl
    ) {
        this.prometheusClient = webClientBuilder
                .baseUrl(prometheusBaseUrl)
                .build();
    }

    @GetMapping("/summary")
    public Mono<ApiResponse<MonitoringSummary>> summary() {
        Mono<Map<String, Double>> cpu = queryVector(NODE_CPU_QUERY);
        Mono<Map<String, Double>> memory = queryVector(NODE_MEMORY_QUERY);

        return Mono.zip(cpu, memory)
                .map(tuple -> ApiResponse.success(buildSummary(tuple.getT1(), tuple.getT2(), null)))
                .onErrorResume(error -> Mono.just(ApiResponse.success(
                        buildSummary(Map.of(), Map.of(), error.getMessage())
                )));
    }

    private Mono<Map<String, Double>> queryVector(String query) {
        return prometheusClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/query")
                        .queryParam("query", query)
                        .build())
                .retrieve()
                .bodyToMono(PrometheusQueryResponse.class)
                .timeout(Duration.ofSeconds(3))
                .map(this::toInstanceValues);
    }

    private Map<String, Double> toInstanceValues(PrometheusQueryResponse response) {
        Map<String, Double> values = new HashMap<>();
        if (response == null || response.data() == null || response.data().result() == null) {
            return values;
        }

        for (PrometheusVectorResult result : response.data().result()) {
            String instance = result.metric().get("instance");
            Double value = parseValue(result.value());
            if (instance != null && value != null) {
                values.put(normalizeInstance(instance), round(value));
            }
        }
        return values;
    }

    private MonitoringSummary buildSummary(
            Map<String, Double> cpuValues,
            Map<String, Double> memoryValues,
            String error
    ) {
        List<String> instances = new ArrayList<>();
        cpuValues.keySet().forEach(instance -> {
            if (!instances.contains(instance)) {
                instances.add(instance);
            }
        });
        memoryValues.keySet().forEach(instance -> {
            if (!instances.contains(instance)) {
                instances.add(instance);
            }
        });

        List<NodeUsage> nodes = instances.stream()
                .sorted()
                .map(instance -> new NodeUsage(
                        instance,
                        cpuValues.getOrDefault(instance, 0.0),
                        memoryValues.getOrDefault(instance, 0.0)
                ))
                .sorted(Comparator.comparing(NodeUsage::cpuPercent).reversed())
                .toList();

        double cpuAverage = average(nodes.stream().map(NodeUsage::cpuPercent).toList());
        double memoryAverage = average(nodes.stream().map(NodeUsage::memoryPercent).toList());

        return new MonitoringSummary(
                error == null,
                Instant.now().toString(),
                new ClusterUsage(round(cpuAverage), round(memoryAverage), nodes.size()),
                nodes,
                error
        );
    }

    private static String normalizeInstance(String instance) {
        int portIndex = instance.lastIndexOf(':');
        return portIndex > 0 ? instance.substring(0, portIndex) : instance;
    }

    private static Double parseValue(List<Object> value) {
        if (value == null || value.size() < 2 || value.get(1) == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.get(1).toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record MonitoringSummary(
            boolean available,
            String timestamp,
            ClusterUsage cluster,
            List<NodeUsage> nodes,
            String error
    ) {
    }

    public record ClusterUsage(
            double cpuPercent,
            double memoryPercent,
            int nodeCount
    ) {
    }

    public record NodeUsage(
            String instance,
            double cpuPercent,
            double memoryPercent
    ) {
    }

    public record PrometheusQueryResponse(
            String status,
            PrometheusData data
    ) {
    }

    public record PrometheusData(
            List<PrometheusVectorResult> result
    ) {
    }

    public record PrometheusVectorResult(
            Map<String, String> metric,
            List<Object> value
    ) {
    }
}
