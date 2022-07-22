package testing;

import processorScheduling.ProcessorExecutionSimulator;

public class SimulatorTest
{
    public static void main(String[] args)
    {
        ProcessorExecutionSimulator simulator = ProcessorExecutionSimulator.getInstance();
        simulator.run();
    }
}
