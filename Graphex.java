/*
 * From a regular expression make a NFA
 * From a NFA make a DFA
 * Perform regular expression operations with the DFA
 * 
 */


package graphex;

import java.util.*;
import java.io.*;


/**
 * Parse the regular expression for correctness
 * @author Greg
 */
class RegexParser {
    private String input;
    private Stack<Regex> stack;
    private int parentheses;
    
    /**
     * Constructor
     * @param regex the string representation of the regular expression 
     */
    public RegexParser(String regex) {
        this.input = regex;
        this.stack = new Stack<Regex>();
        this.parentheses = 0;
    }
    
    /**
     * Parses and builds regular expressions
     * @return the total regular expression
     */
    public Regex parse() { 
        // While there are still characters in the string
        while (moreToParse()) {
            // If it is a left parentheses consume it and add to the parentheses tracker
            if (peekAtString() == '(') {
                consume('(');
                this.parentheses++;
                
                // Then parse whatever is in the parentheses
                parseInStatement();
                
                // Then concatenate whatever was in the parentheses and the existing
                // regular expressions
                while (this.stack.size() > 1) {
                    Regex temp2 = this.stack.pop();
                    Regex temp1 = this.stack.pop();
                    Regex concatenation = new Concatenation(temp1, temp2);
                    this.stack.push(concatenation);
                }
            }
            // If it is a right parentheses consume it and subtract from the parentheses tracker
            else if (peekAtString() == ')') {
                consume(')');
                this.parentheses--;
            }
            // If it is a star cosume it then pop the last regular expression and star it
            else if (peekAtString() == '*') {
                consume('*');
                
                // Do not allow double star
                if (peekAtString() == '*') {
                    System.out.println("Invalid regex");
                    System.exit(0);
                }
                
                Regex star = this.stack.pop();
                star = new Star(star);
                this.stack.push(star);
            }
            // If it is a union pop the last statement then parse the next expression and union them
            else if (peekAtString() == '|') {                
                consume('|');
                
                // Do not allow double union or union star
                if (peekAtString() == '|' || peekAtString() == '*') {
                    System.out.println("Invalid regex");
                    System.exit(0);
                }
                
                Regex part1 = this.stack.pop();
                
                parseInStatement();
                
                Regex part2 = this.stack.pop();
                Regex union = new Union(part1, part2);
                this.stack.push(union);
            }
            // Otherwise it is just a character so make a symbol and push it onto the stack
            else {
                    Regex symbol = new Symbol("" + next());
                    this.stack.push(symbol);
            }
        }
        
        // Concatenate everything in the stack
        while(this.stack.size() > 1) {
            Regex temp2 = this.stack.pop();
            Regex temp1 = this.stack.pop();
            Regex concatenation = new Concatenation(temp1, temp2);
            this.stack.push(concatenation);
        }
        
        // If the parentheses do not match fail
        if (this.parentheses != 0) {
            System.out.println("Invalid regex");
            System.exit(0);
        }
        
        return this.stack.pop();
    }
    
    /**
     * Parses inside a regular expression so that the stack can be preserved
     */
    public void parseInStatement() {
        while (moreToParse()) {
            // If it is a left parentheses consume it and add to the count
            if (peekAtString() == '(') {
                consume('(');
                this.parentheses++;
            }
            // If it is at a right parentheses consume it, decrement the count, and return
            else if (peekAtString() == ')') {
                consume(')');
                this.parentheses--;
                return;
            }
            // If it is a star pop the last thing on the stack and star it
            else if (peekAtString() == '*') {
                consume('*');
                
                if (peekAtString() == '*') {
                    System.out.println("Invalid regex");
                    System.exit(0);
                }
                
                Regex star = this.stack.pop();
                star = new Star(star);
                this.stack.push(star);
            }
            // If it is a union pop the last the on the stack and then parse for the next regular expression and union them
            else if (peekAtString() == '|') {                
                consume('|');
                
                if (peekAtString() == '|' || peekAtString() == '*') {
                    System.out.println("Invalid regex");
                    System.exit(0);
                }
                
                Regex part1 = this.stack.pop();
                
                parseInStatement();
                
                Regex part2 = this.stack.pop();
                Regex union = new Union(part1, part2);
                this.stack.push(union);
            }
            // Otherwise it is just a symbol so add one and put it on the stack
            else {
                    Regex symbol = new Symbol("" + next());
                    this.stack.push(symbol);
            }
        }
    }
    
    /**
     * Get the next character in the string
     * @return the next character or if it is empty return null
     */
    private char peekAtString() {
        if (this.input.length() > 0)
            return this.input.charAt(0);
        else
            return '\0';
    }
    
    /**
     * Checks that the character is next then deletes it from the string
     * @param c the character to be removed from the string
     */
    private void consume(char c) {
        if (peekAtString() == c) {
            this.input = this.input.substring(1);
        }
        else {
            System.out.println("Invalid regular expression");
            System.exit(0);
        }
    }
    
    /**
     * Gets the next character then removes it from the string
     * @return the next character in the string
     */
    private char next() {
        char c = peekAtString();
        consume(c);
        return c;
    }
    
    /**
     * Checks if there is still more in the string
     * @return true if there is more to parse, false otherwise
     */
    private boolean moreToParse() {
        return input.length() > 0;
    }    
}

/**
 * Regular expression abstract class
 * Regular expressions can be symbols, union, concatenation, or star
 * @author Greg
 */
abstract class Regex {
    abstract public NFA createNFA(StateNumber states);
}

/**
 * A single symbol regular expression
 * @author Greg
 */
class Symbol extends Regex {
    private String symbol;
    
    /**
     * Symbol constructor
     * @param symbol a character
     */
    public Symbol (String symbol) {
        this.symbol = symbol;
    }
    
    /**
     * Creates a NFA with a start and end state with a transition on the symbol
     * @param states the current state number
     * @return the NFA created
     */
    @Override
    public NFA createNFA(StateNumber states) {
        int startState = states.getNextStateNumber();
        int acceptState = states.getNextStateNumber();
        
        NFA nfa = new NFA(startState, acceptState);
        nfa.addDeltaTransition(startState, this.symbol, acceptState);
        return nfa;
    }
}

/**
 * Concatenate two regular expressions together
 * @author Greg
 */
class Concatenation extends Regex {
    private Regex firstRegex;
    private Regex secondRegex;
    
    /**
     * Constructor for concatenation
     * @param first the first regular expression
     * @param second the second regular expression
     */
    public Concatenation (Regex first, Regex second) {
        this.firstRegex = first;
        this.secondRegex = second;
    }
    
    /**
     * Creates a NFA with the first regular expression concatenated with the
     * second
     * @param states current state number
     * @return the NFA created
     */
    @Override
    public NFA createNFA(StateNumber states) {
        NFA firstNFA = this.firstRegex.createNFA(states);
        NFA secondNFA = this.secondRegex.createNFA(states);
        
        NFA nfa = new NFA(firstNFA.getStartState(), secondNFA.getAcceptState());
        
        // Add the delta transitions from the NFA of first regular expression 
        // to the one being created
        for (int i = 0; i < firstNFA.getDeltaTransition().size(); i++) {
            nfa.addDeltaTransition(i, firstNFA.getDeltaTransition().get(i));
        }
        
        // Add the delta transitions from the NFA of second regular expression 
        // to the one being created
        for (int i = 0; i < secondNFA.getDeltaTransition().size(); i++) {
            nfa.addDeltaTransition(i, secondNFA.getDeltaTransition().get(i));
        }
        
        // Add an epsilon from the accept state of the first NFA to the start
        // state of the second NFA
        nfa.addDeltaTransition(firstNFA.getAcceptState(), 
                "epsilon", secondNFA.getStartState());
        
        return nfa;
    }
}

/**
 * Union two regular expressions together
 * @author Greg
 */
class Union extends Regex {
    private Regex firstRegex;
    private Regex secondRegex;
    
    /**
     * Constructor for Union
     * @param first the first regular expression
     * @param second the second regular expression
     */
    public Union (Regex first, Regex second) {
        this.firstRegex = first;
        this.secondRegex = second;
    }
    
    /**
     * Creates a NFA that is the union of two regular expressions
     * @param states the current state
     * @return the NFA created
     */
    @Override
    public NFA createNFA(StateNumber states) {
        NFA firstNFA = this.firstRegex.createNFA(states);
        NFA secondNFA = this.secondRegex.createNFA(states);
        
        int startState = states.getNextStateNumber();
        int acceptState = states.getNextStateNumber();
        
        NFA nfa = new NFA(startState, acceptState);
        
        // Add the delta transitions from the NFA of first regular expression 
        // to the one being created
        for (int i = 0; i < firstNFA.getDeltaTransition().size(); i++) {
            nfa.addDeltaTransition(i, firstNFA.getDeltaTransition().get(i));
        }
        
        // Add the delta transitions from the NFA of second regular expression 
        // to the one being created
        for (int i = 0; i < secondNFA.getDeltaTransition().size(); i++) {
            nfa.addDeltaTransition(i, secondNFA.getDeltaTransition().get(i));
        }
        
        // Add a transition from the start state of the NFA being created
        // to the start state of the first NFA and the second NFA
        nfa.addDeltaTransition(startState, 
                "epsilon", firstNFA.getStartState());
        nfa.addDeltaTransition(startState, 
                "epsilon", secondNFA.getStartState());
        
        // Add a transition from the accept state of the first and second NFA
        // to the accept state of the NFA being created
        nfa.addDeltaTransition(firstNFA.getAcceptState(), 
                "epsilon", acceptState);
        nfa.addDeltaTransition(secondNFA.getAcceptState(), 
                "epsilon", acceptState);
        
        return nfa;
    }
}

/**
 * Creates a star of the regular expression
 * @author Greg
 */
class Star extends Regex {
    private Regex regex;
    
    /**
     * Constructor for Star
     * @param regex regular expression supplied
     */
    public Star (Regex regex) {
        this.regex = regex;
    }
    
    /**
     * Creates an NFA with the star property
     * @param states the current state
     * @return the NFA created
     */
    @Override
    public NFA createNFA(StateNumber states) {
        NFA firstNFA = this.regex.createNFA(states);
        
        int startAndAcceptState = states.getNextStateNumber();
        
        NFA nfa = new NFA(startAndAcceptState, startAndAcceptState);
        
        // Add all of the transitions from the regular expression NFA to the one
        // being created
        for (int i = 0; i < firstNFA.getDeltaTransition().size(); i++) {
            nfa.addDeltaTransition(i, firstNFA.getDeltaTransition().get(i));
        }
        
        // Add a transistion from the start state to the start state of the
        // NFA
        nfa.addDeltaTransition(startAndAcceptState, 
                "epsilon", firstNFA.getStartState());
        
        // Add a transistion from the accept state of the first NFA to the 
        // start state of the NFA being created
        nfa.addDeltaTransition(firstNFA.getAcceptState(), 
                "epsilon", startAndAcceptState);
        
        return nfa;
    }
}

/**
 * Transition function keeps track of what transitions allow the DFA/NFA to 
 * move to a different state
 * @author Greg
 */
class Transition {
    private String transitionOn;
    private int targetState;
    
    /**
     * Constructor for Transition
     * @param transition what character is will transition on
     * @param targetState what state it will transition to
     */
    public Transition (String transition, int targetState) {
        this.transitionOn = transition;
        this.targetState = targetState;
    }
    
    /**
     * @return the character that is used in the transition
     */
    public String getTransition() {
        return this.transitionOn;
    }
    
    /**
     * @return the integer state that it will go to on the transition
     */
    public int getTarget() {
        return this.targetState;
    }
    
    /**
     * Override of the toString method for Transition
     * @return the transition and to the state
     */
    @Override
    public String toString() {
        return this.transitionOn + " -> " + this.targetState;
    }
}


/**
 * State number keeps track of the state number so that independent states
 * are made starting a 0
 * @author Greg
 */
class StateNumber {
    private int stateNumber = 0;
    
    /**
     * @return the next state number
     */
    public int getNextStateNumber() {
        return this.stateNumber++;
    }
}

/**
 * Creates an NFA from a given start and accept state with the delta transitions
 * between the states
 * @author Greg
 */
class NFA {
    private int startState;
    private int acceptState;
    private ArrayList<List<Transition>> deltaTransition;
   
    /**
     * Constructor for NFA
     * @param startState integer start state
     * @param exitState integer accept state
     */
    public NFA (int startState, int acceptState) {
        this.startState = startState;
        this.acceptState = acceptState;
        
        this.deltaTransition = new ArrayList<List<Transition>>();
        
        this.deltaTransition.add(new LinkedList<Transition>());
        
        // If the start state equals the accept state there is no need to have
        // transitions to other start states so only add a linked list of 
        // transitons if they are not equal
        if (startState != acceptState) {
            this.deltaTransition.add(new LinkedList<Transition>());        
        }
    }
    
    /**
     * @return the start state
     */
    public int getStartState() {
        return this.startState;
    }
    
    /**
     * @return the accept state
     */
    public int getAcceptState() {
        return this.acceptState;
    }
    
    /**
     * @return the delta transitions
     */
    public ArrayList<List<Transition>> getDeltaTransition() {
        return this.deltaTransition;
    }
    
    /**
     * Adds a transition to the NFA
     * @param state1 the first state
     * @param stateLabel what the NFA transitions on
     * @param state2 the state the the first state transitions to
     */
    public void addDeltaTransition(int state1, String stateLabel, int state2) {
        List<Transition> state1Transition = new LinkedList<Transition>();
        
        // If the state does not exist create states until it does
        if (state1 >= this.deltaTransition.size())
        {
            for (int i = this.deltaTransition.size(); i <= state1; i++)
                this.deltaTransition.add(new LinkedList<Transition>());
        }
        
        state1Transition = this.deltaTransition.get(state1);

        
        // Then add a new transition to the linked list of transitions
        state1Transition.add(new Transition(stateLabel, state2));
    }
    
    /**
     * Adds a transition to the NFA
     * @param transition a map of existing transitions
     */
    public void addDeltaTransition(int state, List<Transition> transition) {
        // If a transition is not already in the NFA create states until it is
        // then add the transition to it
        if (!this.deltaTransition.contains(transition)) {
            for (int i = this.deltaTransition.size(); i <= state; i++)
                this.deltaTransition.add(new LinkedList<Transition>());
            
            this.deltaTransition.add(state, transition);
        }
    }
    
    /**
     * Converts the NFA to a DFA
     * @return a DFA
     */
    public DFA nfaToDFA() {
        // First make all of the transitions for the DFA
        Map<Set<Integer>, Map<String, Set<Integer>>> dfaTransitions = DFATransitions();        
        
        // Get all of the states in the DFA then create an injective map from new integers to be used as the future states
        Set<Set<Integer>> dfaSetOfStates = dfaTransitions.keySet();        
        Map<Set<Integer>, Integer> setToIntegerStates = new HashMap<Set<Integer>, Integer>();
        
        // To create injective map put the set into the last position in the map
        for (Set<Integer> s : dfaSetOfStates) {
            setToIntegerStates.put(s, setToIntegerStates.size());
        }
        
        // Using the injective map replace the set of integers with integers and preserve the transitions to the different states 
        Map<Integer, Map<String, Integer>> DFAIntegerToTransitions = setsOfStatesToSingleStates(setToIntegerStates, dfaTransitions);
        
        // Remove all epsilon states
        Set<Integer> dfaStart = resolveEpsilonClosure(Collections.singleton(this.startState));
        
        // Get the start state
        int DFAStartState = setToIntegerStates.get(dfaStart);
        
        Set<Integer> DFAAccept = new HashSet<Integer>();
        
        // If the set of states contains the accept state add its integer state to the set of accepting states
        for (Set<Integer> state : dfaSetOfStates) {
            if (state.contains(this.acceptState)) {
                DFAAccept.add(setToIntegerStates.get(state));
            }
        }
        
        return new DFA(DFAStartState, DFAAccept, DFAIntegerToTransitions);
        
    }
    
    /**
     * Builds the DFA transitions from the transitions in the NFA
     * @return a map of set of state that contain the character transition and the set of integers that it goes to
     */
    public Map<Set<Integer>, Map<String, Set<Integer>>> DFATransitions() {
        // Create a set of all the sets reachable from the start state by removing epsilon transitions
        Set<Integer> setsFromStartState = resolveEpsilonClosure(Collections.singleton(this.startState));
        
        // Create a list of states and add the intial sets to it
        LinkedList<Set<Integer>> stateList = new LinkedList<Set<Integer>>();
        stateList.add(setsFromStartState);
        
        // Create the map
        Map<Set<Integer>, Map<String, Set<Integer>>> statesAndTransitions = new HashMap<Set<Integer>, Map<String, Set<Integer>>>();
        
        // Go until there are no more states to be processed
        while (!stateList.isEmpty()) {
            // Get the first set of states
            Set<Integer> state = stateList.removeFirst();
            
            // If the set is not already contained in the map add it otherwise continue to the next set
            if (!statesAndTransitions.containsKey(state)) {
                Map<String, Set<Integer>> stateTransitions = new HashMap<String, Set<Integer>>();
                
                // For all the states in the set and there transitions
                for (int s : state) {
                    for (Transition t : this.deltaTransition.get(s)) {
                    
                        // As long as it is not an epsilon transition create a state of set to transition to
                        if (!t.getTransition().equals("epsilon")) {
                            Set<Integer> toState;

                            // If the map already contains the character to be transitioned on retrieve the set of integers
                            if (stateTransitions.containsKey(t.getTransition())) {
                                toState = stateTransitions.get(t.getTransition());
                            }
                            // Otherwise, create a new set and add it to the map
                            else {
                                toState = new HashSet<Integer>();
                                stateTransitions.put(t.getTransition(), toState);
                            }
                            
                            // Then add the current target to the set of states to transition to
                            toState.add(t.getTarget());
                        }
                    }
                }
                
                HashMap<String, Set<Integer>> stateTransitionsClosed = new HashMap<String, Set<Integer>>();
                
                // Then remove all epsilon closures from the set of states in the map
                for (Map.Entry<String, Set<Integer>> element : stateTransitions.entrySet()) {
                    Set<Integer> close = resolveEpsilonClosure(element.getValue());
                    stateTransitionsClosed.put(element.getKey(), close);
                    stateList.add(close);
                }
                
                // Then add the current set of states and there transitions
                statesAndTransitions.put(state, stateTransitionsClosed);
            }
        }
        
        return statesAndTransitions;
    }
    
    /**
     * Removes epsilons transitions from a set of states
     * @param states the set of states in the NFA
     * @return the new set of states with no epsilon transitions
     */
    public Set<Integer> resolveEpsilonClosure(Set<Integer> states) {
        // Get a list of the states in the set and declare a new set to be epsilon free
        LinkedList<Integer> stateList = new LinkedList<Integer>(states);
        Set<Integer> stateSet = new HashSet<Integer>(states);
        
        // While the are still states to be analyzed, get the first one
        while (!stateList.isEmpty()) {
            int s = stateList.removeFirst();
            
            // For each transition from a state, if the transition is an epsilon and the state
            // cannot already reach it add it to the new set of states and the target state to be processed
            for (Transition t : this.deltaTransition.get(s)) {
                if (t.getTransition().equals("epsilon") && !stateSet.contains(t.getTarget())) {
                    stateSet.add(t.getTarget());
                    stateList.add(t.getTarget());
                }
            }
        }
        
        return stateSet;
    }
    
    /**
     * Create a of integer state values and a map of their transitions from character to an integer
     * @param setsToStates the set of states in the DFA
     * @param transitions the transitions in the DFA
     * @return a map from integer states to its transitions
     */
    public Map<Integer, Map<String, Integer>> setsOfStatesToSingleStates(Map<Set<Integer>, Integer> setsToStates, Map<Set<Integer>, Map<String, Set<Integer>>> transitions) {
        Map<Integer, Map<String, Integer>> newTransitions = new HashMap<Integer, Map<String, Integer>>();
        
        // For each transition in the DFA, get the set of states
        for (Map.Entry<Set<Integer>, Map<String, Set<Integer>>> element : transitions.entrySet()) {
            Set<Integer> s = element.getKey();
            Map<String, Integer> newSTransitions = new HashMap<String, Integer>();
            
            // Then put the character transition and the integer mapping for the set of states that it transitions to
            // in the new map created
            for (Map.Entry<String, Set<Integer>> t : element.getValue().entrySet()) {
                newSTransitions.put(t.getKey(), setsToStates.get(t.getValue()));
            }
            
            // Then put all of the transitions for a state in th new map
            newTransitions.put(setsToStates.get(s), newSTransitions);
        }
        
        return newTransitions;
    }
    
    /**
     * Creates the dot file for the NFA
     * @param file the file to be output to
     */
    public void createDotFile(String file) {
        File dotFile = new File(file);
        
        try {
            if (!dotFile.exists())
                dotFile.createNewFile();
            
            // Use print writer for print line function
            PrintWriter writer = new PrintWriter(new FileWriter(dotFile));
           
            // Write the header
            writer.println("digraph NFA {");
            writer.println("rankdir=LF;");
            writer.println("node [shape = none]; \"\";");
            writer.println("node [shape = doublecircle]; q" + this.acceptState + ";");
            writer.println("node [shape = circle];");
            writer.println("\"\" -> q" + this.startState + ";");
            
            // For each state write what it transitions to
            for (int i = 0; i < this.deltaTransition.size(); i++) {
                for (Transition t : this.deltaTransition.get(i)) {
                    writer.println("q" + i + " -> q" + t.getTarget() + " [ label = " + t.getTransition() + " ];");
                }
            }
            
            writer.println("}");
            
            writer.flush();
            writer.close();
        }
        catch (IOException e) {
            System.out.println("Problem writing or creating the file");
        }
        
    }
    
    /**
     * @return the NFA start and accept state then the delta transitions
     */
    @Override
    public String toString() {
        String output = "NFA start = " + this.startState + " accept = " + this.acceptState
                + "\n";
        
        for (int i = 0; i < this.deltaTransition.size(); i++) {
            output += "" + i + " " + this.deltaTransition.get(i).toString() + "\n";
        }
        
        return output;
    }
}

/**
 * Creates a DFA with a start state and a set of accepting state in their transitions
 * @author Greg
 */
class DFA {
    private int startState;
    private Set<Integer> acceptStates;
    private Map<Integer, Map<String, Integer>> deltaTransitions;
    
    /**
     * Constructor for DFA
     * @param startState the start state of the DFA
     * @param acceptStates the set of states that accept
     * @param deltaTransitions the transitions between states
     */
    public DFA(int startState, Set<Integer> acceptStates, Map<Integer, Map<String, Integer>> deltaTransitions) {
        this.startState = startState;
        this.acceptStates = acceptStates;
        this.deltaTransitions = deltaTransitions;
    }
    
    /**
     * @return gets the start state
     */
    public int getStart() {
        return this.startState;
    }
    
    /**
     * @return the set of accepting states
     */
    public Set<Integer> getAcceptStates() {
        return this.acceptStates;
    }
    
    /**
     * @return the transitions in the DFA
     */
    public Map<Integer, Map<String, Integer>> getDeltaTranstions() {
        return this.deltaTransitions;
    }
    
    /**
     * Add transitions to the null state for unused letter
     * @param alphabet the alphabet used in the file
     */
    public void transitionToNullState(int[] alphabet) {
        Map<Integer, Map<String, Integer>> newTransitions = new HashMap<Integer, Map<String, Integer>>();
        
        for (Map.Entry<Integer, Map<String, Integer>> transition : this.deltaTransitions.entrySet()) {
            Map<String, Integer> newNullTransition = new HashMap<String, Integer>();
            
            // For each state in the transition map, check to make sure that it has a transition on every character
            for (int i = 0; i < alphabet.length; i++) {
                // If state does not have a transition for the character add a null transition
                if ((alphabet[i] != 0) && (!transition.getValue().containsKey("" + (char) i))) {
                    newNullTransition.put("" + (char) i, null);
                }
            }
            
            // Add the existing transitions
            for (Map.Entry<String, Integer> t : transition.getValue().entrySet()) {
                newNullTransition.put(t.getKey(), t.getValue());
            }
            
            // Put all of the transitions back in the map
            newTransitions.put(transition.getKey(), newNullTransition);
        }
        
        this.deltaTransitions = newTransitions;
    }
    
    /**
     * Performs the pattern matching on a file
     * @param input the file for processing
     */
    public void performRegexOnFile(String input) {
        File f = new File(input);
        
        if (!f.exists()) {
            System.out.println("Input file does not exist");
            
            System.exit(0);
        }
 
        try {

            String currentLine;   

            BufferedReader reader = new BufferedReader(new FileReader(input));
            
            // Only process line by line
            while ((currentLine = reader.readLine()) != null) {
                // Start at the start state
                int currentState = this.startState;
                // Get the transitions for the start state
                Map<String, Integer> tempState = this.deltaTransitions.get(currentState);
                int newState = -1;
                String match = "";
                
                // Process each character in the string
                for (int i = 0; i < currentLine.length(); i++) {
                    String currentLetter ="" + currentLine.charAt(i);
                    
                    // If the state has a transition that is not null for that character
                    if (tempState.get(currentLetter) != null) {
                        // Add to the current match
                        match += currentLetter;
                        
                        // Get the state that is transitioned to
                        newState = tempState.get(currentLetter);
                        
                        // Get the transitions for that state
                        tempState = this.deltaTransitions.get(newState);
                    }
                    // If it is a transition to null, stop processing the string and make the state "null"
                    else {
                        i = currentLine.length();
                        newState = -1;
                    }
                }
                
                // If the state that is ended on is a accept state print the match
                if (this.acceptStates.contains(newState)) {
                    System.out.println("String matched " + match);
                }
                // If the start state is apart of the accept state and no longer string was matched
                // match the empty string
                else if (this.acceptStates.contains(this.startState)) {
                    System.out.println("Empty String matched");
                }
                
            }
            
            reader.close();

        } catch (IOException e) {
                System.out.println("Problem reading the file");
        }
    }
    
    /**
     * Creates a dot file for the DFA
     * @param file file to be output to
     */
    public void createDotFile(String file) {
        File dotFile = new File(file);
        
        try {
            if (!dotFile.exists())
                dotFile.createNewFile();
            
            // Use print writer for println
            PrintWriter writer = new PrintWriter(new FileWriter(dotFile));
            
            // Header
            writer.println("digraph DFA {");
            writer.println("rankdir=LF;");
            writer.println("node [shape = none]; \"\";");
            
            writer.print("node [shape = doublecircle]; ");
            
            // Get the amount of accepting states
            int counter = 0;
            if (this.acceptStates.size() == 1) {
                counter = 1;
            }
            else {
                counter = this.acceptStates.size();
            }
            
            // Print the accepting states
            for (int state : this.deltaTransitions.keySet()) {
                // If there are more than one print them comma separated
                if (this.acceptStates.contains(state) && counter > 1) {
                    writer.print("q" + state + ", ");
                    counter--;
                }
                // If there is only one or only one left print it followed by the semicolon
                else if (counter == 1 && this.acceptStates.contains(state)) {
                    writer.print("q" + state + ";");
                }
            }
            writer.println();
            
            writer.println("node [shape = circle];");
            writer.println("\"\" -> q" + this.startState + ";");
            
            // Print out all of the transitions
            for (Map.Entry<Integer, Map<String, Integer>> element : this.deltaTransitions.entrySet()) {
                int state = element.getKey();
                
                for (Map.Entry<String, Integer> stateTransitions : element.getValue().entrySet()) {
                    
                    // If the state to transition to is not null include the q otherwise just print null
                    if (stateTransitions.getValue() != null) {
                        writer.println("q" + state + " -> q" + stateTransitions.getValue() + " [ label = " + stateTransitions.getKey() + " ];");
                    }
                    else {
                        writer.println("q" + state + " -> " + stateTransitions.getValue() + " [ label = " + stateTransitions.getKey() + " ];");
                    }
                }
            }
            
            writer.println("}");
            
            writer.flush();
            writer.close();
        }
        catch (IOException e) {
            System.out.println("Problem writing or creating the file");
        }
    }
    
    /**
     * @return the DFA start and accept states then the delta transitions
     */
    @Override
    public String toString() {
        String output = "DFA start = " + this.startState + " accept = " + this.acceptStates
                + "\n" + this.deltaTransitions;
        return output;
    }
}



/**
 * Contains the main method for running the program
 * @author Greg
 */
public class Graphex {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String inputFile = null;
        String regex = null;
        String nfaDotFile = null;
        String dfaDotFile = null;
        int alphabet[] = new int[256];       
        
        if (args.length > 0)  {
            // If the nfa dot file is to be created get the file name
            if (args[0].equals("-n")) {
                nfaDotFile = args[1];
                
                // If the dfa dot file is to be created get the file name
                if (args[2].equals("-d")) {
                    dfaDotFile = args[3];
                    regex = args[4];
                    inputFile = args[5];
                }
                // Otherwise get the regular expression and the file to process
                else {
                    regex = args[2];
                    inputFile = args[3];
                }
            }
            // If the dfa dot file is to be create get the file name and other arguments
            else if (args[0].equals("-d")) {
                dfaDotFile = args[1];
                regex = args[2];
                inputFile = args[3];
            }
            // Otherwise just get the regular expression and the fil to process
            else {
                regex = args[0];
                inputFile = args[1];
            }
        }
        else {
            System.out.println("No commands given");
            
            System.exit(0);
        }        
        
        // Parse the regular expression and get the regular expression
        RegexParser regexParse = new RegexParser(regex);
        Regex r = regexParse.parse();
        
        // Get the alphabet used
        getAlphabet(inputFile, alphabet);
        
        // Create an nfa from the regular expression and then a dfa from the nfa
        NFA nfa = r.createNFA(new StateNumber());
        DFA dfa = nfa.nfaToDFA();
        
        // Add the null transitions given the alphabet
        dfa.transitionToNullState(alphabet);
        
        // If the dot files are to be written to write to them
        if (nfaDotFile != null) {
            nfa.createDotFile(nfaDotFile);
        }
        
        if (dfaDotFile != null) {
            dfa.createDotFile(dfaDotFile);
        }
        
        // Regex the file
        dfa.performRegexOnFile(inputFile);
    }
    
    /**
     * Gets the alphabet of characters used in the file to be processed
     * @param input file to be processed
     * @param alphabet array of the alphabet
     */
    public static void getAlphabet(String input, int[] alphabet) {
        File f = new File(input);
        
        if (!f.exists()) {
            System.out.println("Input file does not exist");
            
            System.exit(0);
        }
 
        try {

            String currentLine;

            BufferedReader reader = new BufferedReader(new FileReader(input));

            // For every character just add it to its ascii position in the array
            while ((currentLine = reader.readLine()) != null) {
                for (int i = 0; i < currentLine.length(); i++) {
                    int asciiCode = (int) currentLine.charAt(i);
                    alphabet[asciiCode]++;
                }
            }
            
            reader.close();

        } catch (IOException e) {
                System.out.println("Problem reading the file");
        }
    }
    
}
