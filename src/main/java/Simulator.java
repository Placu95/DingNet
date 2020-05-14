import gui.MainGUI;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import iot.SimulationRunner;
import iot.SimulationUpdateListener;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.NotNull;
import util.MutableInteger;
import util.SettingsReader;
import util.time.DoubleTime;
import util.time.Time;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Simulator starter class.
 * If the simulator is executed with the option "-i" and the input profile is specified then
 * the simulator starts a timedRun simulation in batch mode (without GUI),
 * otherwise it starts the GUI
 */
public class Simulator {

    // FIXME I don't like it here, it require a better solution
    private static String networkConfigFilePath = null;
    public static Optional<String> getNetworkConfigFilePath() {
        return Optional.ofNullable(networkConfigFilePath);
    }

    public static void main(String[] args) {

        Options options = new Options();

        Option configFile =
            new Option("cf", "configurationFile", true, "path of the configuration file");
        configFile.setRequired(false);
        options.addOption(configFile);
        Option inputProfileFile =
            new Option("ipf", "inputProfileFile", true, "path of the input profiles file");
        inputProfileFile.setRequired(false);
        options.addOption(inputProfileFile);
        Option outputFile =
            new Option("of", "outputFile", true, "path of the output file");
        outputFile.setRequired(false);
        options.addOption(outputFile);
        Option networkConfigFile =
            new Option("nf", "networkConfigFile", true, "path of the network config file");
        networkConfigFile.setRequired(false);
        options.addOption(networkConfigFile);

        CommandLine cmd = null;

        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("allowed arguments", options);
            System.exit(1);
        }

        SimulationRunner simulationRunner = SimulationRunner.getInstance();
        SettingsReader settingsReader = SettingsReader.getInstance();
        // methods to copy all the files with the configuration simulation from the
        // resource directory to the the directory in the user directory
        copyResourceDirectory(settingsReader.getConfigurationsResources(), settingsReader.getConfigurationsDirectory());

        if (cmd.hasOption("inputProfileFile")) {
            simulationRunner.setInputProfiles(cmd.getOptionValue("inputProfileFile"));
        }

        networkConfigFilePath = cmd.getOptionValue("networkConfigFile");

        if (cmd.hasOption("configurationFile")) {
            var file = new File(cmd.getOptionValue("configurationFile"));
            simulationRunner.loadConfigurationFromFile(file);
            simulationRunner.getSimulation().setInputProfile(simulationRunner.getInputProfiles().get(0));
            simulationRunner.setupTimedRun();
            var sec = new MutableInteger(5);
            simulationRunner.simulate(
                sec,
                new BatchSimulationUpdater(sec, cmd.getOptionValue("outputFile")),
                false
            );
        } else {
            MainGUI.startGUI(simulationRunner);
        }
    }

    /**
     * Update listener for the simulation in batch mode
     */
    private static class BatchSimulationUpdater implements SimulationUpdateListener {

        private final MutableInteger rate;
        private final String outputFile;
        private final Time time;

        public BatchSimulationUpdater(MutableInteger rate) {
            this(rate, null);
        }

        public BatchSimulationUpdater(MutableInteger rate, String outputFile) {
            this.rate = rate;
            this.outputFile = outputFile;
            time = DoubleTime.zero();
        }

        @Override
        public void update() {
            time.plusSeconds(rate.intValue());
        }

        @Override
        public void onEnd() {
            if (outputFile != null) {
                SimulationRunner
                    .getInstance()
                    .getProtelisApplication()
                    .storeSimulationResults(outputFile);
            }
        }
    }

    // region copy resources
    // TODO improve error check and refresh file
    private static void copyResourceDirectory(@NotNull String source, @NotNull String destination) {
        var dirDest = new File(destination);
        if (!dirDest.exists()) {
            if (!dirDest.mkdirs()) {
                throw new IllegalStateException("Impossible create directory: " + dirDest.getAbsolutePath());
            }
            try (ScanResult scanResult = new ClassGraph().whitelistPathsNonRecursive(source).scan()) {
                scanResult.getResourcesWithExtension("xml").forEachByteArray((Resource res, byte[] content) -> {
                    var resName = res.getPath().substring(source.length() - 1);
                    try {
                        PrintWriter writer = new PrintWriter(new File(destination, resName));
                        writer.println(new String(content, StandardCharsets.UTF_8));
                        writer.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }
    // endregion
}
