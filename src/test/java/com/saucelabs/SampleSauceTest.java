package com.saucelabs;

import com.saucelabs.common.SauceOnDemandAuthentication;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.junit.runners.Parameterized;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import com.saucelabs.junit.Parallelized;
import com.saucelabs.junit.ConcurrentParameterized;
import com.saucelabs.junit.SauceOnDemandTestWatcher;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.io.File;
import java.lang.Thread;
import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;

import com.saucelabs.common.SauceOnDemandSessionIdProvider;
import com.saucelabs.saucerest.SauceREST;


/**
 * Demonstrates how to write a JUnit test that runs tests against Sauce Labs using multiple browsers in parallel.
 * <p/>
 * The test also includes the {@link SauceOnDemandTestWatcher} which will invoke the Sauce REST API to mark
 * the test as passed or failed.
 *
 * @author Ross Rowe
 */
@SuppressWarnings("unused")
@RunWith(ConcurrentParameterized.class)
public class SampleSauceTest implements SauceOnDemandSessionIdProvider {

    /**
     * Constructs a {@link SauceOnDemandAuthentication} instance using the supplied user name/access key.  To use the authentication
     * supplied by environment variables or from an external file, use the no-arg {@link SauceOnDemandAuthentication} constructor.
     */
	public static final String USERNAME = System.getenv("SAUCE_USERNAME");
	public static final String ACCESS_KEY = System.getenv("SAUCE_ACCESS_KEY");
    public SauceOnDemandAuthentication authentication = new SauceOnDemandAuthentication(USERNAME, ACCESS_KEY);
    
    /**
     * JUnit Rule which will mark the Sauce Job as passed/failed when the test succeeds or fails.
     */
    @Rule
    public SauceOnDemandTestWatcher resultReportingTestWatcher = new SauceOnDemandTestWatcher(this, authentication);

    /**
     * Represents the browser to be used as part of the test run.
     */
    private String browser;
    /**
     * Represents the operating system to be used as part of the test run.
     */
    private String os;
    /**
     * Represents the version of the browser to be used as part of the test run.
     */
    private String version;
    /**
     * Instance variable which contains the Sauce Job Id.
     */
    private String sessionId;

    /**
     * The {@link WebDriver} instance which is used to perform browser interactions with.
     */
    private WebDriver driver;
    
    private SauceREST sauceClient;
    
    @Rule
    public TestName name = new TestName();
    
    private String pageSource;
    

    /**
     * Constructs a new instance of the test.  The constructor requires three string parameters, which represent the operating
     * system, version and browser to be used when launching a Sauce VM.  The order of the parameters should be the same
     * as that of the elements within the {@link #browsersStrings()} method.
     * @param os
     * @param version
     * @param browser
     */
    public SampleSauceTest(String os, String version, String browser) {
        super();
        this.os = os;
        this.version = version;
        this.browser = browser;
    }
    
    /*public SampleSauceTest() {
        super();
    }*/

    /**
     * @return a LinkedList containing String arrays representing the browser combinations the test should be run against. The values
     * in the String array are used as part of the invocation of the test constructor
     */
    @ConcurrentParameterized.Parameters
    public static LinkedList<String[]> browsersStrings() {
        LinkedList<String[]> browsers = new LinkedList<String[]>();
        browsers.add(new String[]{"Windows 7", "42", "chrome"});
        browsers.add(new String[]{"Windows 7", "43", "chrome"});
        browsers.add(new String[]{"Windows 7", "44", "chrome"});
        browsers.add(new String[]{"Windows 7", "45", "chrome"});
        return browsers;
    }

    /**
     * Constructs a new {@link RemoteWebDriver} instance which is configured to use the capabilities defined by the {@link #browser},
     * {@link #version} and {@link #os} instance variables, and which is configured to run against ondemand.saucelabs.com, using
     * the username and access key populated by the {@link #authentication} instance.
     *
     * @throws Exception if an error occurs during the creation of the {@link RemoteWebDriver} instance.
     */
    @Before
    public void setUp() throws Exception {

    	DesiredCapabilities caps = new DesiredCapabilities();
    	ChromeOptions options = new ChromeOptions();
    	//FirefoxProfile profile = new FirefoxProfile();
        //caps.setCapability(CapabilityType.BROWSER_NAME, "chrome");
    	caps.setCapability(CapabilityType.BROWSER_NAME, browser);
        if (version != null) {
            caps.setCapability(CapabilityType.VERSION, version);
        }
        //caps.setCapability(CapabilityType.VERSION, "45");
        caps.setCapability(CapabilityType.PLATFORM, os);
        //caps.setCapability(CapabilityType.PLATFORM, "Windows 7");
        //profile.setPreference("intl.accept_languages", "es");
        //options.addArguments("--lang=es");
        options.addArguments("--start-maximized");
        caps.setCapability(ChromeOptions.CAPABILITY, options);
    	//caps.setCapability("version", "39.0");
        //caps.setCapability("platform", "Windows 7");
        //caps.setCapability(FirefoxDriver.PROFILE, profile);
        caps.setCapability("name", name.getMethodName());
        caps.setCapability("public", "public");
        caps.setCapability("screenResolution", "1680x1050");
        caps.setCapability("maxDuration", "300");
        caps.setCapability("idleTimout", "120");
        caps.setCapability("seleniumVersion", "2.46.0");
        this.driver = new RemoteWebDriver(
                new URL("http://" + authentication.getUsername() + ":" + authentication.getAccessKey() + "@ondemand.saucelabs.com:80/wd/hub"),caps);
        this.sessionId = (((RemoteWebDriver) driver).getSessionId()).toString();
        //this.sauceClient = new SauceREST(authentication.getUsername(), authentication.getAccessKey());
        //this.pageSource = driver.getPageSource();
        //System.out.println(pageSource);

    }

    /**
     * Runs a simple test verifying the title of the amazon.com homepage.
     * @throws Exception
     */
    
    @Test
    public void test32077() throws Exception {
    	driver.get("http://staging.build.com/index.cfm?page=ecrm:login");
    	System.out.print(driver.getCurrentUrl());
    	/*assertEquals("Google", driver.getTitle());
    	WebElement query = driver.findElement(By.name("q"));
        query.sendKeys("Sauce Labs");
        query.submit();*/
        //File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        // Now you can do whatever you need to do with it, for example copy somewhere
        //FileUtils.copyFile(scrFile, new File("c:\\tmp\\screenshot.png"));
    }

    /**
     * Closes the {@link WebDriver} session.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        driver.quit();
        //sauceClient.jobFailed(sessionId);
    }

    /**
     *
     * @return the value of the Sauce Job id.
     */
    @Override
    public String getSessionId() {
        return sessionId;
    }
    
}
