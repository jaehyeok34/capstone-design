package capstone.design;

import capstone.design.broker.Broker;
import capstone.design.topic.Topic;

public class App {

    public static void main(String[] args) {
        Broker.Builder builder  = Broker.builder()
            .port(3401)
            .addTopic("test_topic", Topic.Type.DISK)
            .addTopic("convert_file", Topic.Type.DISK)
            .addTopic("find_join_keys", Topic.Type.DISK)
            .mappingTableFilePath("option_mapping_table.properties");

        try (Broker broker = builder.build()) {
            broker.start();
        } catch (Exception ignored) {}
    }
}
