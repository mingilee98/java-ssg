import java.util.Random;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class Agent {
    int agentID;

    // parameters
    double alpha;
    double gamma;
    double epsilon;

    static int actionCount = 5;
    int stateCount;
    int action;

    double[][] qTable;

    int reward;

    Random rd = new Random(System.currentTimeMillis());

    // create instance and initialize qtable
    public Agent(int agentID, double alpha, double gamma, double epsilon, int stateCount) {
        this.agentID = agentID;
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.stateCount = stateCount;
    }

    void initQTable() {
        qTable = new double[stateCount + 1][actionCount];

        // init qtable with random numbers from 0 to 1.
        for (double[] row : qTable)
            Arrays.fill(row, rd.nextDouble());

    }

    int chooseRandom() {
        int randomNumber = rd.nextInt(actionCount);
        return randomNumber;
    }

    public void getReward(int reward) {
        this.reward = reward;
    }

    int getBestAction(int state) {
        List<Integer> maxValIndexes = new ArrayList<>();
        double maxValue = -10000;

        // get the max from q value
        for (int i = 0; i < actionCount; i++) {
            if (qTable[state][i] >= maxValue) {
                maxValue = qTable[state][i];
            }
        }
        // System.out.println("MaxValue: " + maxValue);

        // add all the max values to the maxVals
        for (int i = 0; i < actionCount; i++) {
            if (qTable[state][i] == maxValue) {
                maxValIndexes.add(i);
            }
        }

        // System.out.println("MaxVals: " + maxVals);

        // return a random action among the actions that has the maximum q
        int randAction = rd.nextInt(maxValIndexes.size());
        return maxValIndexes.get(randAction);

    }

    int chooseEpsilonGreedy(int state) {
        double pos = rd.nextDouble();

        if (pos >= epsilon) {
            return getBestAction(state);
        } else {
            return rd.nextInt(actionCount);
        }
    }

    double getMaxQ(int state) {
        double maxValue = -100000;
        int action = 0;

        // search q value table and return the action with maximum q value
        while (action < actionCount) {
            if (qTable[state][action] > maxValue) {
                maxValue = qTable[state][action];
            }
            action++;
        }
        return maxValue;
    }

    void updateQ(int reward, int curPos, int nextPos) {
        double maxQ = getMaxQ(nextPos);
        double curQ = qTable[curPos][action];
        qTable[curPos][action] = curQ + alpha * (reward + gamma * maxQ - curQ);
    }

    public void makeVote(int state) {
        this.action = chooseEpsilonGreedy(state);
    }

}