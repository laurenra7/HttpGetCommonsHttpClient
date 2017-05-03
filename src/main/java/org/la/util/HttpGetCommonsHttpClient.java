package org.la.util;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * Created by laurenra on 5/31/16.
 */
public class HttpGetCommonsHttpClient {

    private static boolean modeVerbose;

    public static void main(String[] args) throws Exception {

        int exitStatus = 0;
        modeVerbose = false;

        // Build command line options
        Options clOptions = new Options();
        clOptions.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Show this help")
                .build());
        clOptions.addOption(Option.builder("o")
                .longOpt("output")
                .desc("output file")
                .hasArg()
                .argName("filename")
                .build());
        clOptions.addOption(Option.builder("v")
                .longOpt("verbose")
                .desc("show HTTP headers/footers and processing messages")
                .build());

        if(args.length == 0) {
            showCommandHelp(clOptions);
        }
        else {
            exitStatus = processCommandLine(args, clOptions);
        }

        System.exit(exitStatus);
    }


    private static int processCommandLine(String[] args, Options clOptions) {
        int executeStatus = 0;
        String url = "";
        String outputJson = "";

        CommandLineParser clParser = new DefaultParser();


        try {
            CommandLine line = clParser.parse(clOptions, args);

            if (line.hasOption("help")) {
                showCommandHelp(clOptions);
            }
            else {
                if (line.hasOption("verbose")) {
                    modeVerbose = true;
                }

                // Remaining command line parameter(s), if any, is URL
                List<String> cmdLineUrl = line.getArgList();
                if(cmdLineUrl.size() > 0) {
                    url = cmdLineUrl.get(0); // Get only the first parameter as URL, ignore others

                    String response = httpGet(url);
                    if (response != null) {

                        if (line.hasOption("output")) {
                            // Write response to output file
                            executeStatus = writeStringToFile(line.getOptionValue("output"), response);
                        }
                        else {
                            // Write response to console
                            System.out.println(response);
                        }
                    }
                }
                else {
                    System.out.println("Error: no URL");
                    showCommandHelp(clOptions);
                }
            }
        }
        catch (ParseException e) {
            System.err.println("Command line parsing failed. Error: " + e.getMessage() + "\n");
            showCommandHelp(clOptions);
            executeStatus = 1;
        }

        return executeStatus;
    }


    private static String httpGet(String url) {
        HttpClient client = new HttpClient();
        HttpMethod getMethod = new GetMethod(url);
        BufferedReader buffer = null;
        String result = "";

        try {
            int responseCode = client.executeMethod(getMethod);

            if (modeVerbose) {
                result = result + "HTTP response code: " + responseCode + "\n";
            }
            if (responseCode == HttpStatus.SC_OK) {

                if (modeVerbose) {
                    result = result + "---------- Request Header ----------\n";
                    Header[] requestHeaders = getMethod.getRequestHeaders();
                    for (Header reqHeader : requestHeaders) {
                        result = result + reqHeader.getName() + ": " + reqHeader.getValue() + "\n";
                    }

                    result = result + "---------- Response Header ----------\n";
                    Header[] responseHeaders = getMethod.getResponseHeaders();
                    for (Header respHeader : responseHeaders) {
                        result = result + respHeader.getName() + ": " + respHeader.getValue() + "\n";
                    }
                }


                InputStream inStream = getMethod.getResponseBodyAsStream();

                buffer = new BufferedReader(new InputStreamReader(inStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = buffer.readLine()) != null) {
                    stringBuilder.append(line);
                }

                if (modeVerbose) {
                    result = result + "---------- Response Body ----------\n";
                }

                result = result + stringBuilder.toString();

            }
            else {
                System.out.println("Problem with request. HTTP status code: " + responseCode);
            }
        }
        catch (Exception e) {
            System.out.println("Error fetching request: " + e.getMessage());
        }
        finally {
            if (buffer != null) {
                try {
                    buffer.close();
                }
                catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }

        return result;
    }


    private static int writeStringToFile(String outputFilename, String outputString) {
        int status = 0;
        BufferedWriter bufferedWriter = null;
        FileWriter fileWriter = null;

        if (modeVerbose) {
            System.out.println("Output file: " + outputFilename);
        }

        try {
            fileWriter = new FileWriter(outputFilename);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(outputString);

        }
        catch (IOException e) {
            System.out.println("Problem writing to file. Error: " + e.getMessage());
            status = 1;
        }
        finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
            catch (IOException ioErr) {
                System.out.println("Problem closing file. Error: " + ioErr.getMessage());
                status = 1;
            }
        }

        return status;
    }


    private static void showCommandHelp(Options options) {
        String commandHelpHeader = "\nDo an HTTP GET using the Apache Commons HttpClient.\n\n";
        String commandHelpFooter = "\nExamples:\n\n" +
                "  java -jar HttpGetCommonsHttpClient.jar https://someurl.com/get/stuff\n\n" +
                "  java -jar HttpGetCommonsHttpClient.jar -o myfile.txt https://someurl.com/get/stuff\n\n" +
                "  java -jar HttpGetCommonsHttpClient.jar -v https://someurl.com/get/stuff\n\n";

        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(88,"java -jar HttpGetCommonsHttpClient.jar url", commandHelpHeader, options, commandHelpFooter, true);
    }


}
