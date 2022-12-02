import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.CyclicBarrier;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.plugins.tiff.ExifTIFFTagSet;
import javax.sql.rowset.spi.SyncResolver;

public class SSG {

    // grid variables
    static int rowSize = 7;
    static int colSize = 7;
    static int gridSize = rowSize * colSize;
    static int edgeSize = 2;
    static int objWidth = 3;
    static int objHeight = 2;
    static int objSize = objWidth * objHeight;
    static int stateCount = (rowSize - objHeight + 1) * (colSize - objWidth + 1);
    static int stateRowSize = (colSize - objWidth + 1);
    static int initPos = 3;
    static int goalPos = initPos + (rowSize - 2) * (colSize - 2);

    // static int goalPos = 4;
    // static int initPos = goalPos + (rowSize - 2) * (colSize - 2);

    // simulation variables
    static int simCount = 100;
    static int trialCount = 4000;
    static int maxStep = 10000;
    // static int timestepCount = 100;

    // learning variables
    static double alpha = 0.1;
    static double gamma = 0.9;
    static double epsilon = 0.15;
    static boolean decreasingEpsilon = true;

    // reward variables
    static int rewardGoal = 10;
    static int rewardEdge = -50;
    static int rewardElse = 0;

    // set random seed
    static Random rd = new Random(System.currentTimeMillis());

    // directions
    static String[] directions = { "up", "down", "left", "right", "nop" };

    // filename
    static String filename = rowSize + "_" + colSize + "_" + epsilon + (decreasingEpsilon ? "decreasing" : "")
            + "original" + ".csv";

    // gets the agents under the object
    public static Agent[] getAgentsUnder(Agent[] agents, int curAgent) {
        Agent[] agentsUnder = new Agent[objSize];

        for (int i = 0; i < objSize; i++) {
            if (i != 0 && i % objWidth == 0) {
                curAgent = curAgent + colSize - objWidth;
            }
            agentsUnder[i] = agents[curAgent - 1];
            curAgent++;
        }

        return agentsUnder;
    }

    // gets the agents by the object
    public static Agent[] getAgentsBy(Agent[] agents, int curAgent) {
        int agentBySize = objWidth * 2 + objHeight * 2;
        Agent[] agentsBy = new Agent[agentBySize];

        // get up
        int index = 0;
        int tempPos = curAgent - colSize;
        while (index < objWidth) {
            // check out of bounds
            if (tempPos <= 0) {
                index += objWidth;
                break;
            }
            agentsBy[index++] = agents[tempPos - 1];
            tempPos++;
        }

        // get down
        tempPos = curAgent + colSize * 2;
        while (index < objWidth * 2) {
            // check out of bounds
            if (tempPos >= gridSize) {
                index += objWidth;
                break;
            }
            agentsBy[index++] = agents[tempPos - 1];
            tempPos++;
        }

        // get left
        tempPos = curAgent - 1;

        while (index < objWidth * 2 + objHeight) {
            // check out of bounds
            if (curAgent % colSize == 1) {
                index += objHeight;
                break;
            }
            agentsBy[index] = agents[tempPos - 1];
            tempPos += colSize;
            index++;
        }

        tempPos = curAgent + objWidth;
        while (index < objWidth * 2 + objHeight * 2) {
            // check out of bounds
            if ((curAgent + objWidth) % colSize == 1) {
                index += objHeight;
                break;
            }
            agentsBy[index] = agents[tempPos - 1];
            tempPos += colSize;
            index++;
        }

        return agentsBy;
    }

    // gets the directions by computing the votes
    public static int getDirection(int[] votes) {
        int[] sumOfWeight = new int[5]; // up, down, left, right, nop

        // add up the votes
        for (int i = 0; i < objSize; i++) {
            int vote = votes[i];
            if (vote == 0)
                sumOfWeight[0] += 1;
            if (vote == 1)
                sumOfWeight[1] += 1;
            if (vote == 2)
                sumOfWeight[2] += 1;
            if (vote == 3)
                sumOfWeight[3] += 1;
            if (vote == 4)
                sumOfWeight[4] += 1;
        }

        for (int i = objSize; i < votes.length; i++) {
            int vote = votes[i];
            if (vote == -1)
                continue;
            if (vote == 0)
                sumOfWeight[0] += 3;
            if (vote == 1)
                sumOfWeight[1] += 3;
            if (vote == 2)
                sumOfWeight[2] += 3;
            if (vote == 3)
                sumOfWeight[3] += 3;
            if (vote == 4)
                sumOfWeight[4] += 3;
        }

        // cancellation
        Integer[] canceledSum = new Integer[5];
        canceledSum[0] = sumOfWeight[0] - sumOfWeight[1];
        canceledSum[1] = sumOfWeight[1] - sumOfWeight[0];
        canceledSum[2] = sumOfWeight[2] - sumOfWeight[3];
        canceledSum[3] = sumOfWeight[3] - sumOfWeight[2];
        canceledSum[4] = sumOfWeight[4];

        // canceledSum[0] = sumOfWeight[0];
        // canceledSum[1] = sumOfWeight[1];
        // canceledSum[2] = sumOfWeight[2];
        // canceledSum[3] = sumOfWeight[3];
        // canceledSum[4] = sumOfWeight[4];

        // get the max
        List<Integer> maxValIndexes = new ArrayList<>();

        int max = Collections.max(Arrays.asList(canceledSum));

        for (int i = 0; i < canceledSum.length; i++) {
            if (canceledSum[i] == max) {
                maxValIndexes.add(i);
            }
        }

        int randAction = rd.nextInt(maxValIndexes.size());

        return maxValIndexes.get(randAction);
    }

    // get the next position on the grid based on the direction
    public static int getNextPos(int curPos, int direction) {
        if (direction == 2 && (curPos - 1) % stateRowSize != 0) {
            curPos--; // left
        } else if (direction == 3 && (curPos - 1) % stateRowSize != (colSize -
                objWidth)) {
            curPos++; // right
        } else if (direction == 0 && (curPos - 1) / stateRowSize != 0) {
            curPos -= stateRowSize; // up
        } else if (direction == 1 && (curPos - 1) / stateRowSize != (rowSize -
                objHeight)) {
            curPos += stateRowSize; // down
        }
        // if (direction == 2 && (curPos - 1) % stateRowSize != 0) {
        // curPos--; // left
        // } else if (direction == 3 && (curPos - 1) % stateRowSize != (colSize -
        // objWidth)) {
        // curPos++; // right
        // } else if (direction == 0 && (curPos - 1) / stateRowSize != 0) {
        // curPos -= stateRowSize; // up
        // } else if (direction == 1 && (curPos - 1) / stateRowSize != (rowSize -
        // objHeight)) {
        // curPos += stateRowSize; // down
        // }

        return curPos;
    }

    // get the next reward based on the next position
    public static int getNextReward(int nextPos) {
        int reward;
        if ((nextPos - 1) % stateRowSize == 0 || (nextPos - 1)
                % stateRowSize == (colSize - objWidth)) {
            reward = rewardEdge; // edge
        } else if (nextPos == goalPos) {
            reward = rewardGoal; // goal
        } else {
            reward = rewardElse; // default
        }
        return reward;
    }

    // create file
    public static File createFile() {
        File file = null;
        try {
            file = new File(filename);
            if (file.createNewFile()) {
                System.out.println("File created: " + file.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return file;
    }

    // write to file
    public static void writeToFile(String line) {
        try {
            FileWriter myWriter = new FileWriter(filename, true);
            myWriter.write(line);
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    // gets the agent ID at pos
    public static int getAgentID(int pos) {
        int rowIndex = (pos - 1) / stateRowSize;
        int offset = (pos - 1) % stateRowSize;
        return rowIndex * colSize + offset + 1;
    }

    public static void main(String[] args) {

        // create file
        File file = createFile();

        // start simulation
        for (int curSim = 0; curSim < simCount; curSim++) {
            // create agents
            Random rd = new Random(System.currentTimeMillis());
            Agent agents[] = new Agent[gridSize];
            for (int i = 0; i < gridSize; i++) {
                agents[i] = new Agent(i + 1, alpha, gamma, epsilon, stateCount, rd.nextLong());
                agents[i].initQTable();
            }

            // int a = 1 / 0;
            // start trial
            for (int curTrial = 0; curTrial < trialCount; curTrial++) {
                // create line for the file
                StringJoiner line = new StringJoiner(" ");
                line.add(String.valueOf(curSim + 1));
                line.add(String.valueOf(curTrial + 1));

                int curPos = initPos;
                int steps = 0;
                int totalReward = 0;

                // set epsilon value if decreasing and reset number of votes
                for (Agent agent : agents) {
                    if ((curTrial + 1) % 2 == 0) {// if even trial, only exploit
                        agent.epsilon = 0.0;
                    }
                    if (decreasingEpsilon) { // if decreasing epsilon, change the epsilon value
                        agent.epsilon = agent.epsilon / (1 + Math.exp(curTrial / 2500 - 2));
                    }
                    agent.numOfVotes = 0;
                }

                while (curPos != goalPos && maxStep > steps) {
                    steps++;

                    // get the agent at curPos
                    int curAgent = getAgentID(curPos);

                    // get the agents under the object
                    Agent agentsUnder[] = getAgentsUnder(agents, curAgent);

                    // get the agents by the object
                    Agent agentsBy[] = getAgentsBy(agents, curAgent);

                    // make everyone vote
                    for (Agent agent : agents) {
                        agent.makeVote(curPos);
                        // System.out.print(agent.action + " ");
                    }
                    // int a = 1 / 0;
                    // get the votes from voters
                    int[] votes = new int[agentsUnder.length + agentsBy.length];
                    int index = 0;

                    for (Agent agentUnder : agentsUnder) {
                        votes[index++] = rd.nextDouble() < 0.0001 ? rd.nextInt(5) : agentUnder.action;
                        agentUnder.numOfVotes++;
                    }

                    for (Agent agentBy : agentsBy) {
                        if (agentBy == null) {
                            votes[index++] = -1;
                            continue;
                        }
                        votes[index++] = rd.nextDouble() < 0.0001 ? rd.nextInt(5) : agentBy.action;
                        agentBy.numOfVotes++;
                    }

                    // determine the direction and calculate reward
                    int direction = getDirection(votes);

                    int nextPos = getNextPos(curPos, direction);

                    int nextReward = getNextReward(nextPos);

                    for (Agent agent : agents) {
                        if ((curTrial + 1) % 2 == 1) {
                            agent.getReward(nextReward);
                            agent.updateQ(nextReward, curPos, nextPos);
                        }

                    }

                    // checks the position of agents under and by for each timestep

                    line.add("curPos: ");
                    line.add(String.valueOf(curPos));
                    line.add("direction: ");
                    line.add(String.valueOf(direction));
                    line.add("nextPos: ");
                    line.add(String.valueOf(nextPos));

                    // line.add("agents under: ");

                    // for (Agent agentUnder : agentsUnder) {
                    // line.add(String.valueOf(agentUnder.agentID));
                    // }

                    // line.add("agents by: ");

                    // for (Agent agentBy : agentsBy) {
                    // if (agentBy == null) {
                    // continue;
                    // }
                    // line.add(String.valueOf(agentBy.agentID));
                    // }

                    totalReward += nextReward;
                    curPos = nextPos;
                }

                // write information to the line
                line.add(String.valueOf(steps));
                line.add(String.valueOf(totalReward));
                for (int i = 0; i < gridSize; i++) {
                    line.add(String.valueOf(agents[i].numOfVotes));
                }
                String lineString = line.toString();
                lineString += "\n";
                writeToFile(lineString);

                System.out
                        .println("simulation " + (curSim + 1) + " trial " + (curTrial + 1) + " is over with " + steps
                                + " steps");
                // if (curTrial + 1 == 1) {
                // System.out.println("trial 1 " + Arrays.toString(agents[0].qTable[3]));
                // }
                // if (curTrial + 1 == 1000) {
                // System.out.println("trial 100 " + Arrays.toString(agents[0].qTable[3]));
                // }

            }
        }

    }
}
