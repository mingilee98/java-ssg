import java.util.Random;
import java.util.ArrayList;

public class Agent {
    int agentID;

    // parameters
    double alpha;
    double gamma;
    double epsilon;

    static int actionCount = 5;
    int action;

    double[][] qTable;

    Random rd = new Random();

    // create instance and initialize qtable
    public Agent(int agentID, double alpha, double gamma, double epsilon, int stateCount) {
        this.agentID = agentID;
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        qTable = new double[stateCount][actionCount];
    }

    int chooseRandom() {
        int randomNumber = rd.nextInt(actionCount);
        return randomNumber;
    }

    // int getBestAction() {
    // }

    // int chooseEpsilonGreedy() {
    // }

    public void makeVote() {
        this.action = chooseRandom();
    }

}