package world.bentobox.biomes.web;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.World;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.biomes.BiomesAddon;
import world.bentobox.biomes.web.objects.LibraryEntry;


/**
 * This class manages content downloading from web repository.
 */
public class WebManager
{
    /**
     * Default constructor
     *
     * @param addon StoneGeneratorAddon object.
     */
    public WebManager(BiomesAddon addon)
    {
        this.addon = addon;
        this.plugin = addon.getPlugin();

        this.library = new ArrayList<>(0);

        if (this.plugin.getSettings().isGithubDownloadData())
        {
            long connectionInterval = this.plugin.getSettings().getGithubConnectionInterval() * 20L * 60L;

            if (connectionInterval <= 0)
            {
                // If below 0, it means we shouldn't run this as a repeating task.
                this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin,
                    () -> this.requestCatalogGitHubData(true),
                    600L);
            }
            else
            {
                // Set connection interval to be at least 60 minutes.
                connectionInterval = Math.max(connectionInterval, 60 * 20 * 60L);
                this.plugin.getServer().getScheduler().runTaskTimerAsynchronously(this.plugin,
                    () -> this.requestCatalogGitHubData(true),
                    600L,
                    connectionInterval);
            }
        }
    }


    /**
     * This method requests catalog entries from magic cobblestone generator library.
     *
     * @param clearCache Boolean that indicates if all cached values must be cleared.
     */
    public void requestCatalogGitHubData(boolean clearCache)
    {
        this.plugin.getWebManager().getGitHub().ifPresent(gitHubWebAPI ->
        {
            if (this.plugin.getSettings().isLogGithubDownloadData())
            {
                this.plugin.log("Downloading data from GitHub...");
            }

            String catalogContent = "";

            // Downloading the data
            try
            {
                catalogContent = gitHubWebAPI.getRepository("BentoBoxWorld", "weblink").
                    getContent("biomes/catalog.json").
                    getContent().replaceAll("\\n", "");
            }
            catch (IllegalAccessException e)
            {
                if (this.plugin.getSettings().isLogGithubDownloadData())
                {
                    this.plugin.log("Could not connect to GitHub.");
                }
            }
            catch (Exception e)
            {
                this.plugin.logError("An error occurred when downloading data from GitHub...");
                this.plugin.logStacktrace(e);
            }

            // People were concerned that the download took ages, so we need to tell them it's over now.
            if (this.plugin.getSettings().isLogGithubDownloadData())
            {
                this.plugin.log("Successfully downloaded data from GitHub.");
            }

            // Decoding the Base64 encoded contents
            catalogContent = new String(Base64.getDecoder().decode(catalogContent),
                StandardCharsets.UTF_8);

            /* Parsing the data */

            // Register the catalog data
            if (!catalogContent.isEmpty())
            {
                if (clearCache)
                {
                    this.library.clear();
                }

                JsonObject catalog = new JsonParser().parse(catalogContent).getAsJsonObject();
                catalog.getAsJsonArray("biomes").forEach(gamemode ->
                    this.library.add(LibraryEntry.fromJson(gamemode.getAsJsonObject())));
            }
        });
    }


    /**
     * This method requests GitHub data for given LibraryEntry object.
     *
     * @param user User who inits request.
     * @param world Target world where challenges should be loaded.
     * @param entry Entry that contains information about requested object.
     */
    public void requestEntryGitHubData(User user, World world, LibraryEntry entry)
    {
        this.plugin.getWebManager().getGitHub().ifPresent(gitHubWebAPI ->
        {
            if (this.plugin.getSettings().isLogGithubDownloadData())
            {
                this.plugin.log("Downloading data from GitHub...");
            }

            String biomesLibrary = "";

            // Downloading the data
            try
            {
                biomesLibrary = gitHubWebAPI.getRepository("BentoBoxWorld", "weblink").
                    getContent("biomes/library/" + entry.repository() + ".json").
                    getContent().
                    replaceAll("\\n", "");
            }
            catch (IllegalAccessException e)
            {
                if (this.plugin.getSettings().isLogGithubDownloadData())
                {
                    this.plugin.log("Could not connect to GitHub.");
                }
            }
            catch (Exception e)
            {
                this.plugin.logError("An error occurred when downloading data from GitHub...");
                this.plugin.logStacktrace(e);
            }

            // People were concerned that the download took ages, so we need to tell them it's over now.
            if (this.plugin.getSettings().isLogGithubDownloadData())
            {
                this.plugin.log("Successfully downloaded data from GitHub.");
            }

            // Decoding the Base64 encoded contents
            biomesLibrary = new String(Base64.getDecoder().decode(biomesLibrary),
                StandardCharsets.UTF_8);

            /* Parsing the data */

            // Process downloaded library data
            if (!biomesLibrary.isEmpty())
            {
                this.addon.getImportManager().processDownloadedFile(user,
                    world,
                    biomesLibrary);
            }
        });
    }


// ---------------------------------------------------------------------
// Section: Getters
// ---------------------------------------------------------------------


    /**
     * This method returns all library entries that are downloaded.
     *
     * @return existing Library entries.
     */
    public List<LibraryEntry> getLibraryEntries()
    {
        List<LibraryEntry> entries = new ArrayList<>(this.library);
        entries.sort(Comparator.comparingInt(LibraryEntry::slot));

        return entries;
    }


    /**
     * This static method returns if GitHub data downloader is enabled or not.
     *
     * @return {@code true} if data downloader is enabled, {@code false} - otherwise.
     */
    public static boolean isEnabled()
    {
        return BentoBox.getInstance().getWebManager().getGitHub().isPresent();
    }


// ---------------------------------------------------------------------
// Section: Variables
// ---------------------------------------------------------------------

    /**
     * BiomesAddon variable.
     */
    private final BiomesAddon addon;

    /**
     * BentoBox plugin variable.
     */
    private final BentoBox plugin;

    /**
     * This list contains all entries that were downloaded from GitHub.
     */
    private final List<LibraryEntry> library;
}