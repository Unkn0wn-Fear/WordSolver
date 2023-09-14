import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.util.List;

public class WordSearchApp extends JFrame {
    private JTextField[][] grid;
    private JTextField maxWordLengthEdit;
    private JTextField minWordLengthEdit;

    private JPanel foundWordsContainer;
    private Set<String> dictionary;
    private Trie dictionaryTrie;
    private String selectedWord;
    private Set<String> foundWords;

    private static final String DICTIONARY_FILE = "words.txt";

    private JPanel paginationPanel;
    private int currentPage = 0;
    private int pageSize = 20;
    private boolean[][] visited;

    public WordSearchApp() {
        initUI();
    }

    private void initUI() {
        setTitle("Word Search");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 600);

        JPanel mainPanel = new JPanel(new BorderLayout());
        setContentPane(mainPanel);

        JPanel gridPanel = new JPanel(new GridLayout(9, 9));
        grid = new JTextField[9][9];
        initGrid(gridPanel);

        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));

        JPanel maxWordLengthPanel = new JPanel();
        maxWordLengthPanel.add(new JLabel("Maximum Word Length (2 integers):"));
        maxWordLengthEdit = createLimitedTextField(2, 50);
        maxWordLengthPanel.add(maxWordLengthEdit);

        JPanel minWordLengthPanel = new JPanel();
        minWordLengthPanel.add(new JLabel("Minimum Word Length (2 integers):"));
        minWordLengthEdit = createLimitedTextField(2, 50);
        minWordLengthPanel.add(minWordLengthEdit);

        // Add the maximum and minimum word length panels to controlsPanel
        controlsPanel.add(maxWordLengthPanel);
        controlsPanel.add(minWordLengthPanel);

        JButton findButton = new JButton("Find Words");
        findButton.addActionListener(e -> findWords());

        JPanel foundWordsLabelPanel = new JPanel();
        foundWordsLabelPanel.add(new JLabel("Found Words:"));

        foundWordsContainer = new JPanel(new GridLayout(0, 5));

        controlsPanel.add(findButton);
        controlsPanel.add(foundWordsLabelPanel);
        controlsPanel.add(foundWordsContainer);

        mainPanel.add(gridPanel, BorderLayout.CENTER);
        mainPanel.add(controlsPanel, BorderLayout.SOUTH);

        paginationPanel = new JPanel();
        JButton prevPageButton = new JButton("Prev Page");
        JButton nextPageButton = new JButton("Next Page");

        prevPageButton.addActionListener(e -> showPreviousPage());
        nextPageButton.addActionListener(e -> showNextPage());

        paginationPanel.add(prevPageButton);
        paginationPanel.add(nextPageButton);

        mainPanel.add(paginationPanel, BorderLayout.NORTH);

        dictionary = loadDictionary(DICTIONARY_FILE);
        dictionaryTrie = new Trie();
        for (String word : dictionary) {
            dictionaryTrie.insert(word);
        }

        selectedWord = null;
        foundWords = new HashSet<>();
        visited = new boolean[9][9];
    }

    private void initGrid(JPanel panel) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                JTextField textField = createLimitedTextField(1, 30);
                textField.setBackground(Color.LIGHT_GRAY);
                panel.add(textField);
                grid[row][col] = textField;
            }
        }
    }

    private JTextField createLimitedTextField(int limit, int preferredWidth) {
        JTextField textField = new JTextField();
        PlainDocument doc = (PlainDocument) textField.getDocument();
        doc.setDocumentFilter(new JTextFieldLimit(limit));
        textField.setPreferredSize(new Dimension(preferredWidth, textField.getPreferredSize().height));
        return textField;
    }

    private Set<String> loadDictionary(String dictionaryFile) {
        Set<String> words = new HashSet<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(dictionaryFile));
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line.trim().toLowerCase());
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return words;
    }

    private void findWords() {
        String maxWordLengthStr = maxWordLengthEdit.getText();

        if (maxWordLengthStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a maximum word length.", "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int maxWordLength = Integer.parseInt(maxWordLengthStr);

        String[][] gridLetters = new String[9][9];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                gridLetters[row][col] = grid[row][col].getText().toLowerCase();
            }
        }

        foundWords.clear();

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                String currentLetter = gridLetters[row][col];
                if (currentLetter != null && !currentLetter.isEmpty()) {
                    visited[row][col] = true;
                    dfs(gridLetters, row, col, new StringBuilder(currentLetter), -1, -1, visited, maxWordLength);
                    visited[row][col] = false;
                }
            }
        }

        currentPage = 0;
        clearGridHighlights();
        displayFoundWords();
    }

    private void dfs(String[][] grid, int row, int col, StringBuilder currentWord, int prevRow, int prevCol,
            boolean[][] visited, int maxWordLength) {
        if (currentWord.length() > maxWordLength) {
            return;
        }

        if (dictionaryTrie.search(currentWord.toString())) {
            foundWords.add(currentWord.toString());
        }

        int[] dx = { -1, 1, 0, 0 };
        int[] dy = { 0, 0, -1, 1 };

        for (int i = 0; i < 4; i++) {
            int newRow = row + dx[i];
            int newCol = col + dy[i];

            if (newRow >= 0 && newRow < 9 && newCol >= 0 && newCol < 9 &&
                    (newRow != prevRow || newCol != prevCol) && !visited[newRow][newCol]) {
                String nextLetter = grid[newRow][newCol];
                if (!nextLetter.isEmpty()) {
                    visited[newRow][newCol] = true;
                    currentWord.append(nextLetter);
                    dfs(grid, newRow, newCol, currentWord, row, col, visited, maxWordLength);
                    currentWord.deleteCharAt(currentWord.length() - 1);
                    visited[newRow][newCol] = false;
                }
            }
        }
    }

    private void showPreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            displayFoundWords();
        }
    }

    private void showNextPage() {
        int totalWords = foundWords.size();
        int totalPages = (int) Math.ceil((double) totalWords / pageSize);

        if (currentPage < totalPages - 1) {
            currentPage++;
            displayFoundWords();
        }
    }

    private void displayFoundWords() {
        for (int i = foundWordsContainer.getComponentCount() - 1; i >= 0; i--) {
            foundWordsContainer.remove(i);
        }

        List<String> wordsToDisplay = new ArrayList<>(foundWords);

        int startIndex = currentPage * pageSize;
        int endIndex = Math.min(startIndex + pageSize, wordsToDisplay.size());

        for (int i = startIndex; i < endIndex; i++) {
            String word = wordsToDisplay.get(i);
            if (word.length() >= Integer.parseInt(minWordLengthEdit.getText())) { // Check minimum word length
                JButton foundWordButton = new JButton(word);
                foundWordButton.addActionListener(e -> highlightWord(word));
                foundWordsContainer.add(foundWordButton);
            }
        }

        foundWordsContainer.revalidate();
        foundWordsContainer.repaint();
    }

    private void highlightWord(String word) {
        clearGridHighlights();
        selectedWord = word.toLowerCase();

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                String letter = grid[row][col].getText().toLowerCase();
                if (letter.equals(selectedWord.substring(0, 1))) {
                    if (dfsHighlight(row, col, 0, selectedWord)) {
                        return;
                    }
                }
            }
        }
    }

    private boolean dfsHighlight(int row, int col, int index, String targetWord) {
        if (index == targetWord.length()) {
            return true;
        }

        if (row < 0 || row >= 9 || col < 0 || col >= 9) {
            return false;
        }

        String currentLetter = grid[row][col].getText().toLowerCase();
        String targetLetter = targetWord.substring(index, index + 1);

        if (!currentLetter.equals(targetLetter)) {
            return false;
        }

        Color originalColor = grid[row][col].getBackground();
        grid[row][col].setBackground(Color.RED);

        int[] dx = { -1, 1, 0, 0 };
        int[] dy = { 0, 0, -1, 1 };

        for (int i = 0; i < 4; i++) {
            int newRow = row + dx[i];
            int newCol = col + dy[i];

            if (dfsHighlight(newRow, newCol, index + 1, targetWord)) {
                return true;
            }
        }

        grid[row][col].setBackground(originalColor);

        return false;
    }

    private void clearGridHighlights() {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                grid[row][col].setBackground(Color.LIGHT_GRAY);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WordSearchApp wordSearchApp = new WordSearchApp();
            wordSearchApp.setVisible(true);
        });
    }

    private class TrieNode {
        Map<Character, TrieNode> children;
        boolean isEndOfWord;

        TrieNode() {
            children = new HashMap<>();
            isEndOfWord = false;
        }
    }

    private class Trie {
        private TrieNode root;

        Trie() {
            root = new TrieNode();
        }

        void insert(String word) {
            TrieNode node = root;
            for (char ch : word.toCharArray()) {
                node.children.putIfAbsent(ch, new TrieNode());
                node = node.children.get(ch);
            }
            node.isEndOfWord = true;
        }

        boolean search(String word) {
            TrieNode node = root;
            for (char ch : word.toCharArray()) {
                if (!node.children.containsKey(ch)) {
                    return false;
                }
                node = node.children.get(ch);
            }
            return node.isEndOfWord;
        }
    }

    private class JTextFieldLimit extends DocumentFilter {
        private int limit;

        JTextFieldLimit(int limit) {
            super();
            this.limit = limit;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String str, AttributeSet attr)
                throws BadLocationException {
            if ((fb.getDocument().getLength() + str.length()) <= limit) {
                super.insertString(fb, offset, str, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String str, AttributeSet attr)
                throws BadLocationException {
            if ((fb.getDocument().getLength() + str.length() - length) <= limit) {
                super.replace(fb, offset, length, str, attr);
            }
        }
    }
}
