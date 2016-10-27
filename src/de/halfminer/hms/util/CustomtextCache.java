package de.halfminer.hms.util;

import de.halfminer.hms.exception.CachingException;
import org.bukkit.ChatColor;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache that reads customtext file formats, used by Cmdcustomtext and managed by HanStorage
 * Format:
 * #chapter subchapter,chapteralt subchapteralt
 * Text
 * If "Text" ends with a space char, consider the next line as continuation of current line
 * Chapters are not case sensitive and aliases are separated with comma
 * The '&' character will be replaced with Bukkit's color code
 */
public class CustomtextCache {

    private final File file;
    private long lastCached;
    private Map<String, List<String>> cache;

    public CustomtextCache(File file) {
        this.file = file;
    }

    public List<String> getChapter(String chapter) throws CachingException {

        if (wasModified()) reCacheFile();

        if (cache.size() == 0)
            throw new CachingException(CachingException.Reason.FILE_EMPTY);

        String lowercaseChapter = chapter.toLowerCase();
        if (lowercaseChapter.length() == 0 || !cache.containsKey(lowercaseChapter))
            throw new CachingException(CachingException.Reason.CHAPTER_NOT_FOUND);

        return cache.get(lowercaseChapter);
    }

    private void reCacheFile() throws CachingException {

        try {
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            ParseHelper helper = new ParseHelper(fileReader);
            lastCached = file.lastModified();
            this.cache = helper.getCache();

        } catch (CachingException e) {
            throw e;
        } catch (Exception e) {
            throw new CachingException(CachingException.Reason.CANNOT_READ);
        }
    }

    private boolean wasModified() {
        return file.lastModified() > lastCached;
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

                        // remove leading/trailing whitespace and lowercase (not case sensitive)
                        for (String chapter : chapters)
                            if (chapter.length() > 1)
                                currentChapters.add(chapter.trim().toLowerCase());

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
                throw new CachingException(CachingException.Reason.CANNOT_READ);
            }
        }

        Map<String, List<String>> getCache() {
            return cache;
        }

        private void storeInCache() throws CachingException {

            if (currentLine.length() > 0) currentContent.add(currentLine);

            if (currentChapters == null || currentContent.size() == 0) {
                clearCurrent();
                return;
            }

            for (String chapter : currentChapters) {

                if (chapter.length() == 0 || cache.containsKey(chapter))
                    throw new CachingException(CachingException.Reason.SYNTAX_ERROR, lineNumberLastHashtag);

                cache.put(chapter, currentContent);
            }

            clearCurrent();
        }

        private void clearCurrent() {
            currentChapters = new ArrayList<>();
            currentContent = new ArrayList<>();
            currentLine = "";
        }
    }
}
