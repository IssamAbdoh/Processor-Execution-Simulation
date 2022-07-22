package processorScheduling;

import priorities.TaskPriority;
import states.ProcessorState;
import states.TaskState;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Scanner;

public class ProcessorExecutionSimulator implements Simulator
{
    private static int current_cycle = 0;
    private static int number_of_processors;
    private static int number_of_tasks;
    private static final ArrayList<Processor> processors = new ArrayList<>();
    private static final ArrayList<Task> coming_tasks = new ArrayList<>();
    private static final ArrayList<Task> completed_tasks = new ArrayList<>();
    private static final HashSet<Task> high_priority_set = new HashSet<>();
    private static final HashSet<Task> low_priority_set = new HashSet<>();
    private static final StringBuilder output_string = new StringBuilder();
    private static ProcessorExecutionSimulator simulator = null;
    
    private ProcessorExecutionSimulator()
    {
    
    }
    
    public static ProcessorExecutionSimulator getInstance()
    {
        if(simulator == null)
        {
            simulator = new ProcessorExecutionSimulator();
        }
        return simulator;
    }
    
    public void run()
    {
        getInput();
        Scheduler.run();
        printOutput();
    }
    
    private void sortByCreatingTime(ArrayList<Task> list)
    {
        list.sort( Comparator.comparingInt( Task::getCreation_time ) );
    }
    
    private void getInput()
    {
        try
        {
            File file = new File( "input.txt" );
            Scanner sc = new Scanner( file );
            int line_number = 1;
            while(sc.hasNext())
            {
                if(line_number == 1)
                {
                    number_of_processors = Integer.parseInt( sc.nextLine().trim() );
                }
                else if(line_number == 2)
                {
                    number_of_tasks = Integer.parseInt( sc.nextLine().trim() );
                }
                else
                {
                    String line = sc.nextLine().trim();
                    String[] tokens = line.split( " " );
                    int creation_time = Integer.parseInt( tokens[0] );
                    int requested_time = Integer.parseInt( tokens[1] );
                    TaskPriority taskPriority;
                    if(tokens[2].equalsIgnoreCase( "low" ))
                    {
                        taskPriority = TaskPriority.LOW;
                    }
                    else
                    {
                        taskPriority = TaskPriority.HIGH;
                    }
                    coming_tasks.add( new Task( creation_time , requested_time , taskPriority ) );
                }
                
                line_number++;
            }
            System.out.println( "Input file has been read successfully !" );
            sc.close();
            
            for(int i = 0 ; i < number_of_processors ; i++)
            {
                processors.add( new Processor() );
            }
            
            sortByCreatingTime( coming_tasks );
        }
        catch(FileNotFoundException e)
        {
            System.out.println( "Input file is not found !" );
        }
    }
    
    private static void printOutput()
    {
        try
        {
            FileWriter myWriter = new FileWriter( "output.txt" );
            myWriter.write( output_string.toString() );
            myWriter.close();
            System.out.println( "Successfully wrote to the file." );
        }
        catch(IOException e)
        {
            System.out.println( "An error occurred while trying to write on output file !" );
        }
    }
    
    private static class Scheduler
    {
        private Scheduler()
        {
            
        }
        
        public static void run()
        {
            while(notFinished())
            {
                output_string.append( "At cycle = " + current_cycle + "\n" );
                checkArrivedTasks();
                schedule();
                executeProcessors();
                output_string.append( "\n" );
                current_cycle++;
            }
            output_string.append( "PROGRAM IS FINISHED .\n" );
        }
        
        private static boolean notFinished()
        {
            return completed_tasks.size() != number_of_tasks;
        }
        
        private static void executeProcessors()
        {
            for(Processor processor : processors)
            {
                if(processor.isBusy())
                {
                    processor.executeTask();
                }
            }
        }
        
        private static void schedule()
        {
            assignIdleProcessors();
            bringJusticeAmongTasks();
            solveTieBreaking();
        }
        
        private static void solveTieBreaking()
        {
            //NOW tie breaking between high and low
            if(isHighPriorityWaiting() && allProcessorsAssignedHigh())
            {
                for(Processor processor : processors)
                {
                    high_priority_set.add( processor.removeTask() );
                }
                assignIdleProcessors();
            }
        }
        
        private static boolean allProcessorsAssignedHigh()
        {
            for(int i = 0 ; i < number_of_processors ; i++)
            {
                if(processors.get( i ).isBusy() && processors.get( i ).getTask().isLowPriority())
                {
                    return false;
                }
                else if(processors.get( i ).isNotBusy())
                {
                    return false;
                }
            }
            return true;
        }
        
        private static void bringJusticeAmongTasks()
        {
            if(isHighPriorityWaiting() && isLowPriorityAssigned())
            {
                for(Processor processor : processors)
                {
                    if(processor.getTask().isLowPriority())
                    {
                        if(getFirstHighPriorityWaitingTask() != null)
                        {
                            replaceTasks( processor , getFirstHighPriorityWaitingTask() );
                        }
                        else
                        {
                            break;
                        }
                    }
                }
            }
        }
        
        private static Task getFirstHighPriorityWaitingTask()
        {
            if(!high_priority_set.isEmpty())
            {
                return getFirstElement( high_priority_set );
            }
            return null;
        }
        
        private static void assignIdleProcessors()
        {
            for(Processor processor : processors)
            {
                if(processor.isIdle() && !high_priority_set.isEmpty())
                {
                    processor.assignTask( getFirstElement( high_priority_set ) );
                    high_priority_set.remove( getFirstElement( high_priority_set ) );
                }
            }
            
            for(Processor processor : processors)
            {
                if(processor.isIdle() && !low_priority_set.isEmpty())
                {
                    processor.assignTask( getFirstElement( low_priority_set ) );
                    low_priority_set.remove( getFirstElement( low_priority_set ) );
                }
            }
        }
        
        private static void checkArrivedTasks()
        {
            for(int i = 0 ; i < coming_tasks.size() ; i++)
            {
                if(coming_tasks.get( i ).getCreation_time() == current_cycle)
                {
                    output_string.append( "Task id = " + coming_tasks.get( i ).getId() + " has arrived \n" );
                    if(coming_tasks.get( i ).isLowPriority())
                    {
                        low_priority_set.add( coming_tasks.get( i ) );
                    }
                    else
                    {
                        high_priority_set.add( coming_tasks.get( i ) );
                    }
                    coming_tasks.remove( i );
                    i--;
                }
                else
                {
                    break;
                }
            }
        }
        
        private static boolean isHighPriorityWaiting()
        {
            return !high_priority_set.isEmpty();
        }
        
        private static boolean isLowPriorityAssigned()
        {
            for(Processor processor : processors)
            {
                if(processor.getTask().isLowPriority())
                {
                    return true;
                }
            }
            return false;
        }
        
        private static void replaceTasks(Processor processor , Task new_task)
        {
            output_string.append( "Processor id = " + processor.getId() + " has switched from task id = " + processor.getTask().getId() + " to task id = " + new_task.getId() + "\n" );
            processor.getTask().setWaiting();
            if(processor.getTask().isHighPriority())
            {
                high_priority_set.add( processor.getTask() );
            }
            else
            {
                low_priority_set.add( processor.getTask() );
            }
            processor.assignTask( new_task );
            
            if(new_task.isHighPriority())
            {
                high_priority_set.remove( new_task );
            }
            else
            {
                low_priority_set.remove( new_task );
            }
        }
        
        private static Task getFirstElement(HashSet<Task> tasks)
        {
            for(Task task : tasks)
            {
                return task;
            }
            return null;
        }
    }
    
    private static class Processor
    {
        private Task task;
        private static int incremented_Processor_id = 1;
        private ProcessorState state;
        private final int id;
        
        public Task getTask()
        {
            return task;
        }
        
        public ProcessorState getState()
        {
            return state;
        }
        
        public int getId()
        {
            return id;
        }
        
        public Processor(Task task)
        {
            this();
            this.task = task;
            state = ProcessorState.BUSY;
        }
        
        public Processor()
        {
            this.id = incremented_Processor_id++;
            this.state = ProcessorState.IDLE;
        }
        
        public void assignTask(Task task)
        {
            this.task = task;
            this.task.setExecuting();
            this.state = ProcessorState.BUSY;
            output_string.append( "Processor id = " + this.getId() + " has been assigned task id = " + this.task.getId() + "\n" );
        }
        
        public void executeTask()
        {
            if(this.task == null)
            {
                throw new RuntimeException( "No task is assigned to process with id = " + this.id );
            }
            output_string.append( "Processor id = " + this.id + " is executing task id = " + this.task.getId() + "\n" );
            this.task.run();
            if(this.task.isCompleted())
            {
                this.task.setCompleted();
                completed_tasks.add( this.task );
                output_string.append( "Processor id = " + this.id + " is has completed task id = " + this.task.getId() + " and now is idle \n" );
                this.task = null;
                this.state = ProcessorState.IDLE;
            }
        }
        
        public boolean isIdle()
        {
            return this.state == ProcessorState.IDLE;
        }
        
        public boolean isNotIdle()
        {
            return this.state != ProcessorState.IDLE;
        }
        
        public boolean isBusy()
        {
            return this.state == ProcessorState.BUSY;
        }
        
        public boolean isNotBusy()
        {
            return this.state != ProcessorState.BUSY;
        }
        
        public Task removeTask()
        {
            Task task = this.task;
            this.task.setWaiting();
            this.task = null;
            this.state = ProcessorState.IDLE;
            
            return task;
        }
    }
    
    private static class Task implements Comparable<Task>
    {
        private static int incremented_task_id = 1;
        private final int id;
        private final int creation_time;
        private int requested_time;
        private int completion_time;
        private TaskState state;
        private final TaskPriority taskPriority;
        
        public Task(int creation_time , int requested_time , TaskPriority taskPriority)
        {
            this.id = incremented_task_id++;
            this.creation_time = creation_time;
            this.requested_time = requested_time;
            this.taskPriority = taskPriority;
            this.state = TaskState.WAITING;
        }
        
        public void run()
        {
            if(this.requested_time > 0 && this.state != TaskState.COMPLETED)
            {
                this.requested_time--;
                if(this.requested_time == 0)
                {
                    this.state = TaskState.COMPLETED;
                    this.completion_time = current_cycle;
                }
            }
            else
            {
                throw new RuntimeException( "Cannot execute a COMPLETED task !" );
            }
        }
        
        public void setExecuting()
        {
            state = TaskState.EXECUTING;
        }
        
        public void setCompleted()
        {
            state = TaskState.COMPLETED;
        }
        
        public void setWaiting()
        {
            state = TaskState.WAITING;
        }
        
        public int getId()
        {
            return id;
        }
        
        public int getCreation_time()
        {
            return creation_time;
        }
        
        public int getRequested_time()
        {
            return requested_time;
        }
        
        public int getCompletion_time()
        {
            return completion_time;
        }
        
        public TaskState getState()
        {
            return state;
        }
        
        public TaskPriority getPriority()
        {
            return taskPriority;
        }
        
        public boolean isCompleted()
        {
            return state == TaskState.COMPLETED;
        }
        
        public boolean isNotCompleted()
        {
            return state != TaskState.COMPLETED;
        }
        
        private boolean isHighPriority()
        {
            return this.taskPriority == TaskPriority.HIGH;
        }
        
        private boolean isLowPriority()
        {
            return this.taskPriority == TaskPriority.LOW;
        }
        
        @Override
        public int compareTo(Task o)
        {
            /*
             * first we compare based on :
             * taskPriority
             * then creation time
             * then requested time
             * */
            if(this.taskPriority != o.taskPriority)
            {
                if(this.isHighPriority())
                {
                    return -1;
                }
                else
                {
                    return 1;
                }
            }
            if(this.creation_time != o.creation_time)
            {
                return this.creation_time - o.creation_time;
            }
            return this.requested_time - o.requested_time;
        }
    }
}
