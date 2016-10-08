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
 * #chapter subchapter|chapteralt subchapteralt
 * Text
 * If "Text" ends with a space char, consider the next line as continuation of current line
 * Chapter aliases are separated with '|' character
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

        if (!cache.containsKey(chapter))
            throw new CachingException(CachingException.Reason.CHAPTER_NOT_FOUND);

        return cache.get(chapter);
    }

    private void reCacheFile() throws CachingException {

        try {
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            ParseHelper helper = new ParseHelper(fileReader);
            lastCached = file.lastModified();
            this.cache = helper.getCache();

        } catch (Exception e) {
            throw new CachingException(CachingException.Reason.CANNOT_READ);
        }
    }

    private boolean wasModified() {
        return file.lastModified() > lastCached;
    }

    private class ParseHelper {

        private final Map<String, List<String>> cache = new HashMap<>();

        private String[] currentChapters = null;
        private List<String> currentContent = new ArrayList<>();
        private String currentLine = "";

        ParseHelper(BufferedReader reader) throws CachingException {

            try {

                String line;
                while ((line = reader.readLine()) != null) {

                    if (line.startsWith("#")) {

                        storeInCache();
                        currentChapters = line
                                .substring(1)           // remove #
                                .replaceAll(" +", " ")  // replace spaces with single space
                                .split("\\|");          // split at '|' character

                        // remove leading/trailing whitespace
                        for (int i = 0; i < currentChapters.length; i++)
                            currentChapters[i] = currentChapters[i].trim();

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

            if (currentChapters == null) {
                clearCurrent();
                return;
            }

            if (currentLine.length() > 0) currentContent.add(currentLine);

            for (String chapter : currentChapters) {

                if ((chapter.length() == 0 && currentContent.size() > 0) || cache.containsKey(chapter))
                    throw new CachingException(CachingException.Reason.SYNTAX_ERROR);

                cache.put(chapter, currentContent);
            }

            clearCurrent();
        }

        private void clearCurrent() {
            currentChapters = null;
            currentLine = "";
            currentContent = new ArrayList<>();
        }
    }
}
