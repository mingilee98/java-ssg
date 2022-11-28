import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;

import javax.imageio.plugins.tiff.ExifTIFFTagSet;
import javax.sql.rowset.spi.SyncResolver;

public class SSG {

    // grid variables
    static int rowSize = 7;
    static int colSize = 7;
    static int gridSize = rowSize * colSize;
    static int edgeSize = 1;
    static int objWidth = 3;
    static int objHeight = 2;
    static int objSize = objWidth * objHeight;
    static int stateCount = (rowSize - objHeight + 1) * (colSize - objWidth + 1);
    static int stateRowSize = (colSize - objWidth + 1);
    static int initPos = 3;
    static int goalPos = 28;

    // simulation variables
    static int simCount = 1;
    static int trialCount = 100;
    static int timestepCount = 100;

    // reward variables
    static int rewardGoal = 100;
    static int rewardEdge = -50;
    static int rewardElse = -1;

    // learning variables
    static double alpha = 0.1;
    static double gamma = 0.9;
    static double epsilon = 0.1;

    // random variable
    static Random rd = new Random(System.currentTimeMillis());

    static String[] directions = { "up", "down", "left", "right", "nop" };

    public static Agent[] getAgentsUnder(Agent[] agents, int curPos) {
        Agent[] agentsUnder = new Agent[objSize];

        for (int i = 0; i < objSize; i++) {
            if (i != 0 && i % objWidth == 0) {
                curPos = curPos + colSize - objWidth;
            }
            agentsUnder[i] = agents[curPos - 1];
            curPos++;
        }

        return agentsUnder;
    }

    public static Agent[] getAgentsBy(Agent[] agents, int curPos) {
        int agentBySize = objWidth * 2 + objHeight * 2;
        Agent[] agentsBy = new Agent[agentBySize];

        // get up
        int index = 0;
        int tempPos = curPos - colSize;
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
        tempPos = curPos + colSize * 2;
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
        tempPos = curPos - 1;
        while (index < objWidth * 2 + objHeight) {
            // check out of bounds
            if (tempPos % colSize == 0) {
                index += objHeight;
                break;
            }
            agentsBy[index] = agents[tempPos - 1];
            tempPos += colSize;
            index++;
        }

        // get right
        tempPos = curPos + objWidth;
        while (index < objWidth * 2 + objHeight * 2) {
            // check out of bounds
            if (tempPos % colSize == 1) {
                index += objHeight;
                break;
            }
            agentsBy[index] = agents[tempPos - 1];
            tempPos += colSize;
            index++;
        }

        return agentsBy;
    }

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

        // get the max
        List<Integer> maxValIndexes = new ArrayList<>();

        int max = Collections.max(Arrays.asList(canceledSum));

        for (int i = 0; i < canceledSum.length; i++) {
            if (canceledSum[i] == max) {
                maxValIndexes.add(i);
            }
        }

        int randAction = rd.nextInt(maxValIndexes.size());

        // System.out.println(Arrays.toString(sumOfWeight));
        // System.out.println(Arrays.toString(canceledSum));

        return maxValIndexes.get(randAction);
    }

    public static int getNextPos(int curPos, int direction) {
        if (direction == 2 && (curPos - 1) % stateRowSize != 0) {
            curPos--; // left
        } else if (direction == 3 && (curPos - 1) % stateRowSize != (colSize - objWidth)) {
            curPos++; // right
        } else if (direction == 0 && (curPos - 1) / stateRowSize != 0) {
            curPos -= stateRowSize; // up
        } else if (direction == 1 && (curPos - 1) / stateRowSize != (rowSize - objHeight)) {
            curPos += stateRowSize; // down
        }

        return curPos;
    }

    public static int getNextReward(int nextPos) {
        int reward;
        if ((nextPos - 1) % stateRowSize == 0 || (nextPos - 1)
                % stateRowSize == (colSize - objWidth)) {
            reward = -50; // edge
        } else if (nextPos == goalPos) {
            reward = 100; // goal
        } else {
            reward = -1; // default
        }
        return reward;
    }

    public static void main(String[] args) {
        // create agents
        Agent agents[] = new Agent[gridSize];
        for (int i = 0; i < gridSize; i++) {
            agents[i] = new Agent(i + 1, alpha, gamma, epsilon, stateCount);
            agents[i].initQTable();
        }

        // start simulation
        for (int curSim = 0; curSim < simCount; curSim++) {
            // start trial
            for (int curTrial = 0; curTrial < trialCount; curTrial++) {
                int curPos = initPos;
                int steps = 0;
                while (curPos != goalPos) {
                    steps++;
                    // get the agents under the object
                    Agent agentsUnder[] = getAgentsUnder(agents, curPos);
                    // for (Agent a : agentsUnder) {
                    // System.out.println("Agents under:" + a.agentID);
                    // }

                    // get the agents by the object
                    Agent agentsBy[] = getAgentsBy(agents, curPos);
                    // for (Agent a : agentsBy) {
                    // if (a != null) {
                    // System.out.println("Agents by: " + a.agentID);
                    // }
                    // }

                    // make everyone vote
                    for (Agent agent : agents) {
                        agent.makeVote(curPos);
                    }

                    // get the votes from voters
                    int[] votes = new int[agentsUnder.length + agentsBy.length];
                    int index = 0;
                    for (Agent agentUnder : agentsUnder) {
                        votes[index++] = agentUnder.action;
                    }
                    for (Agent agentBy : agentsBy) {
                        if (agentBy == null) {
                            votes[index++] = -1;
                            continue;
                        }
                        votes[index++] = agentBy.action;
                    }

                    // determine the direction and calculate reward
                    int direction = getDirection(votes);
                    int nextPos = getNextPos(curPos, direction);
                    int nextReward = getNextReward(nextPos);

                    for (Agent agent : agents) {
                        agent.getReward(nextReward);
                        agent.updateQ(nextReward, curPos, nextPos);
                    }

                    curPos = nextPos;
                    // System.out.println("current position: " + curPos);
                }
                System.out.println("trial " + curTrial + " is over with " + steps + " steps");
            }
        }

    }
}
