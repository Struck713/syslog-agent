package com.noah.syslog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.noah.syslog.client.Client;
import com.noah.syslog.client.UDPClient;
import com.noah.syslog.config.Config;
import com.noah.syslog.config.ConfigFilter;
import com.noah.syslog.config.ConfigHost;
import com.noah.syslog.log.LogManager;
import com.noah.syslog.message.Message;
import com.noah.syslog.message.enums.Encodings;
import com.noah.syslog.message.enums.Priority;
import com.noah.syslog.message.enums.Severity;
import com.noah.syslog.util.FileUtil;
import com.noah.syslog.util.OSUtil;
import com.noah.syslog.util.WindowsUtil;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SyslogAgent {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final File CONFIG_FILE = new File("config.json");

    public static void main(String[] args) throws IOException, InterruptedException {
        Config config = SyslogAgent.loadConfig();
        LogManager logManager = new LogManager(config.getSources(), config.getFilters());

        ConfigHost host = config.getHost();
        final String hostname = OSUtil.getName();
        Client client = new UDPClient(host.getInetAddress(), host.getPort());
        System.out.println("Sending " + host.getProtocol() + " messages on " + host.getAddress() + ":" + host.getPort() + " hostname, " + hostname);

        final long timeBetweenReads = config.getTimeBetweenReads();
        while (true) {
            List<WindowsUtil.EventLogRecord> nextRecords = logManager.next();
            if (nextRecords.isEmpty()) continue;

            nextRecords.stream().map(record -> {
                String data = record.getStrings() == null ? "No data attached" : Arrays.toString(record.getStrings());
                String source = record.getSource();
                Severity severity = Severity.of(record.getType());
                int statusCode = record.getStatusCode();

                String message = severity.getName() + " from " + source + " with status code " + statusCode + ": " + data;
                return new Message(
                        Priority.LOG_ALERT.with(severity),
                        record.getDate(),
                        hostname,
                        record.getSource(),
                        statusCode,
                        record.getRecordNumber(),
                        Encodings.ANY,
                        message
                );
            }).forEach(message -> {
                client.send(message);
                System.out.println(message);
            });

            Thread.sleep(timeBetweenReads);
        }

    }

    public static Config loadConfig() throws IOException {
        if (!CONFIG_FILE.exists()) {
            InputStream configStream = SyslogAgent.class
                    .getClassLoader()
                    .getResourceAsStream("config.json");
            InputStreamReader isReader = new InputStreamReader(configStream);
            BufferedReader reader = new BufferedReader(isReader);
            String rawData = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            isReader.close();
            reader.close();

            FileUtil.write(rawData, CONFIG_FILE);
        }

        String rawData = FileUtil.read(CONFIG_FILE);
        return SyslogAgent.GSON.fromJson(rawData, Config.class);
    }

}