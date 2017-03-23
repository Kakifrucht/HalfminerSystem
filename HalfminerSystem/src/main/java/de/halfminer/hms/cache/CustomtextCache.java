package de.halfminer.hms.cache;

import de.halfminer.hms.exceptions.CachingException;
import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;

import java.io.*;
import java.util.*;

/**
 * Cache that reads customtext file formats, managed by {@link de.halfminer.hms.handler.HanStorage}.
 * It is automatically reread on file updates.<br>
 * Format:<br>
 * #chapter subchapter,chapteralt subchapteralt<br>
 * Text<br>
 * - If "Text" ends with a space char, consider the next line as continuation of current line<br>
 * - Chapters are not case sensitive and aliases are separated with comma, they may contain wildcards with '*' character<br>
 * - They may also contain aliases in between, such as #chapter subchapter|subchapteralias<br>
 * - The '&' character will be replaced with Bukkit's color code<br>
 */
public class CustomtextCache {

    private final File file;
    private long lastCached;
    private Map<String, List<String>> cache;

    public CustomtextCache(File file) {
        this.file = file;
    }

    List<String> getChapter(String chapter) throws CachingException {
        return getChapter(new String[]{chapter});
    }

    public List<String> getChapter(String[] chapter) throws CachingException {

        reCacheFile();
        if (cache.size() == 0)
            throw new CachingException(file.getName(), CachingException.Reason.FILE_EMPTY);

        String[] chapterLowercase = new String[chapter.length];
        for (int i = 0; i < chapter.length; i++)
            chapterLowercase[i] = chapter[i].toLowerCase();

        // check for literal argument and if not found, for wildcards
        String currentChapter = Utils.arrayToString(chapterLowercase, 0, false);
        for (int i = chapterLowercase.length; i >= 0; i--) {
            if (cache.containsKey(currentChapter))
                return cache.get(currentChapter);
            if (i > 0)
                currentChapter = Utils.arrayToString(chapterLowercase, 0, i, false) + " *";
        }

        throw new CachingException(file.getName(), CachingException.Reason.CHAPTER_NOT_FOUND);
    }

    Set<String> getAllChapters() throws CachingException {
        reCacheFile();
        return cache.keySet();
    }

    private void reCacheFile() throws CachingException {

        if (!wasModified()) return;

        try {
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            ParseHelper helper = new ParseHelper(fileReader);
            lastCached = file.lastModified();
            this.cache = helper.getCache();

        } catch (CachingException e) {
            throw e;
        } catch (Exception e) {
            throw new CachingException(file.getName(), CachingException.Reason.CANNOT_READ);
        }
    }

    private boolean wasModified() {
        return file.lastModified() > lastCached;
    }

    boolean wasModifiedSince(long since) {
        return file.lastModified() > since;
    }

    private class ParseHelper {

        private final Map<String, List<String>> cache = new HashMap<>();

        private int lineNumber = 0;
        private int lineNumberLastHashtag = 0;
        private List<String> currentChapters = new ArrayList<>();
        private List<String> currentContent = new ArrayList<>();
        private String currentLine = "";

        ParseHelper(BufferedReader reader) throws CachingException {

            try {

                String line;
                while ((line = reader.readLine()) != null) {

                    lineNumber++;
                    if (line.startsWith("#")) {

                        storeInCache();
                        String[] chapters = line
                                .substring(1)           // remove #
                                .replaceAll(" +", " ")  // replace spaces with single space
                                .split(",");            // split at komma

                        for (String chapter : chapters) {
                            // remove leading/trailing whitespace and lowercase (not case sensitive)
                            String trimmed = chapter.trim().toLowerCase();
                            // add to current chapters, parse additional "|" seperated aliases
                            currentChapters.addAll(parseChaptersToList(trimmed));
                        }

                        lineNumberLastHashtag = lineNumber;

                    } else {

                        String lineTranslated = ChatColor.translateAlternateColorCodes('&', line);
                        if (line.endsWith(" ")) {
                            currentLine += lineTranslated;
                        } else {

                            if (currentLine.length() > 0) {
                                lineTranslated = currentLine + lineTranslated;
                                currentLine = "";
                            }
                            currentContent.add(lineTranslated);
                        }
                    }
                }

                storeInCache();

            } catch (IOException e) {
                throw new CachingException(file.getName(), CachingException.Reason.CANNOT_READ);
            }
        }

        Map<String, List<String>> getCache() {
            return cache;
        }

        private void storeInCache() throws CachingException {

            if (currentLine.length() > 0) currentContent.add(currentLine);

            if (currentChapters.size() == 0 || currentContent.size() == 0) {
                clearCurrent();
                return;
            }

            for (String chapter : currentChapters) {

                if (chapter.length() == 0 || cache.containsKey(chapter))
                    throw new CachingException(file.getName(), CachingException.Reason.SYNTAX_ERROR,
                            lineNumberLastHashtag, chapter);

                cache.put(chapter, currentContent);
            }

            clearCurrent();
        }

        /**
         * Takes a pre parsed chapter string and creates every combination when splitting arguments at '|' character
         * as aliases. For example: String "test test2|test3|test4" will create a list that contains "test test2",
         * "test test3" and "test test4". It may contain more split arguments such as "test|test2 test3|test4|test5" etc.
         *
         * @param chapters pre parsed chapter string to split further
         * @return list containing every combination of given string
         */
        private List<String> parseChaptersToList(String chapters) {

            List<String> toReturn = new ArrayList<>();

            // array containing commands split at space char
            String[] argumentsSplit = chapters.split(" ");
            // list containing a pair, where left value is current index and right value is further split array at '|' char
            List<Pair<Integer, String[]>> allArgumentsSplit = new ArrayList<>(argumentsSplit.length);

            for (String splitAtSpace : argumentsSplit)
                allArgumentsSplit.add(new Pair<>(0, splitAtSpace.split("\\|")));

            do {
                String addToList = "";
                for (Pair<Integer, String[]> currentPair : allArgumentsSplit) {
                    int index = currentPair.getLeft();
                    addToList += currentPair.getRight()[index] + " ";
                }

                if (addToList.length() > 0) {
                    // cut last space char
                    addToList = addToList.substring(0, addToList.length() - 1);
                    toReturn.add(addToList);
                }

            } while (incrementCounter(allArgumentsSplit));

            return toReturn;
        }

        /**
         * Increments a list containing pairs of current indices, returning false if every combination has been met.
         * It will modify the index part of the list passed.
         *
         * @param counterListToIncrement list containing a pair consisting of current index and array of strings
         * @return true if list was incremented and false if algorithm should terminate
         */
        private boolean incrementCounter(List<Pair<Integer, String[]>> counterListToIncrement) {

            for (Pair<Integer, String[]> currentPair : counterListToIncrement) {
                int currentIndex = currentPair.getLeft();
                if (currentIndex < currentPair.getRight().length - 1) {
                    currentPair.setLeft(currentIndex + 1);
                    return true;
                } else currentPair.setLeft(0);
            }
            return false;
        }

        private void clearCurrent() {
            currentChapters = new ArrayList<>();
            currentContent = new ArrayList<>();
            currentLine = "";
        }
    }
}
