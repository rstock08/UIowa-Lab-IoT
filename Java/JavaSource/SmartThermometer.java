/* Date: 9/28/2017
 * Authors: Reed, Aaron, RJ and Junhan
 * SmartThermometer: 
 * Description: 
*/

// Import statements/libraries
import static java.lang.String.valueOf;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import static javafx.application.Application.launch;
import javafx.geometry.Side;
import javafx.scene.text.Font;
import javafx.util.Duration;

// Main SmartThermometer class
public class SmartThermometer extends Application {
    
    NumberAxis xAxis = new NumberAxis(-300, 0, 2);
    final NumberAxis yAxis = new NumberAxis(10, 50, 2);
    LineChart chart;
    private XYChart.Series<Number, Number> dataSeries;
    private final Timeline animation;
    private double sequence = 0;
    private double y = 10;
    double newPoint;
    int i = 0;
    int F = 1;
    
    private double MAX_DATA_POINTS = 300;
    double MAX = 50;
    double MIN = 10;
    String maxText = "Temp exceeded Max";
    String minText = "Temp dropped below Min";
    boolean messageSent = false;
    boolean overMax = false;
    boolean belowMin = false;
    boolean flag = true;
    boolean buttonFlag = false;
    
    public Arduino arduino = new Arduino("com4", 9600);
    public String arduinoString = "";
    public int arduinoVal = 0;
    
    public static final String ACCOUNT_SID = "AC2b167678a842f3e11cad1df32d5787d8";
    public static final String AUTH_TOKEN = "a53a018d1379b133d8b33e6bb7440b56";
    String phoneNumber = "+16417997438";
    String numCheck = "^[0-9]{10}$";
    int ForC = 1;
    String tempString;
    double temp;
    TextField tempBox = new TextField();
    int x = 0;
    boolean celcius = true;
    int counter = 0;
    boolean switchoff = false;
    boolean unplugged = false;
   
    // Constructor
    public SmartThermometer() {
        
        // Try to open connection
        arduino.openConnection();
        
        this.chart = new LineChart<>(xAxis, yAxis);

        // create timeline to add new data every 60th of second
        animation = new Timeline();
        animation.getKeyFrames().add(new KeyFrame(Duration.millis(1000),(ActionEvent actionEvent) -> plotTime(arduino)));
        animation.setCycleCount(Animation.INDEFINITE);
    }

    // Live update for graph
    private void plotTime(Arduino arduino) {
        boolean isData = dataSeries.getData().add(new XYChart.Data<>(++sequence, newPoint = getNextValue(arduino)));
        
        // Checks if flag is true wont send another message for some time
        if (flag == true){
            counter++;
            System.out.println("her1");
            if (counter > 10){
                flag = false;
                counter=0;
                System.out.println("her2");
            }
        }
        
        // Move shift all values 1 by x axis
        for (int i = dataSeries.getData().size() - 1; i>0; i--){
            dataSeries.getData().get(i).setYValue(dataSeries.getData().get(i-1).getYValue());
        } 
        if(newPoint == -100){
            dataSeries.getData().get(0).setYValue(null);
            dataSeries.getData().remove(dataSeries.getData().get(0).getNode());
        }
        else{
            dataSeries.getData().get(0).setYValue(newPoint); 
            temp = (double) newPoint;
        }
        
        // Updates current temperature of GUI
        if (celcius == true){
            if (switchoff == false && unplugged == false){
                tempBox.setText(temp + " Degrees");
            }
            
        }
        else if (switchoff == false && unplugged == false) {
        
            tempBox.setText(temp*1.8+32 + " Degrees");
        }
        
        // Checks for temperatures too hot
        if(temp > MAX && newPoint != -100 && flag == false){
            System.out.println("The message has sent");
            sendMessage(phoneNumber, maxText, temp);
            overMax = true;
            flag = true;
        }else if(temp < MIN && newPoint != -100 && flag == false){
            System.out.println("The message has sent");
            sendMessage(phoneNumber, minText, temp);
            belowMin = true;
            flag = true;
        }
    }

    // Gets next value from Arduino
    private int getNextValue(Arduino arduino){  
        
        // No connection available
        if (arduino.openConnection() == false){
            switchoff = true;
            switchOff();
            return (-100);
        }
        // Connection available
        else{
            switchoff = false;
            // Reads input from Arduino
            arduinoString = arduino.serialRead();
            arduinoVal = 0;
            System.out.println("made it here 4");
            System.out.println(arduinoString);
            arduinoString = arduinoString.replaceAll("\\D+","");

            // Switch statement that checks values coming from Arduino
            switch (arduinoString) {
                // No information passed from Arduino
                case "":
                    System.out.println("made it here 2");
                    //unplugged = false;
                    return (-100);
                // No sensor connected to Arduino
                case "20002000":
                    System.out.println("Error No Sensor");
                    unplugged = true;
                    unpluggedSensor();
                    return (-100);
                // Value read in
                default:
                    unplugged = false;
                    arduinoVal = Integer.parseInt(arduinoString);
                    arduinoString = "";
                    return (arduinoVal - 273);
            }
        }
    }

    // Updates/plays live animation for graph
    public void play() {
        animation.play();
    }

    // Stops animation
    @Override
    public void stop() {
        animation.pause();
    }
    
    // Method called when sensor is unplugged
    public void unpluggedSensor(){
        tempBox.clear();
        tempBox.setText("Unplugged Sensor");
    }
    
    // method called when no third box available
    public void switchOff(){
        tempBox.setText("No Data Available");
    }

    // Starts graph
    @Override
    public void start(Stage primaryStage) throws Exception {

        yAxis.setSide(Side.RIGHT);

        // setup chart
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setTitle("Temperature Graph");
        xAxis.setLabel("Seconds Ago");
        xAxis.setForceZeroInRange(false);

        yAxis.setLabel("Temp in C");

        // add starting data
        dataSeries = new XYChart.Series<>();
        dataSeries.setName("Data");

        // create some starting data
        boolean isData;
        isData = dataSeries.getData().add(new XYChart.Data<>(--sequence, y));

        chart.getData().add(dataSeries);
        
        TextField number = new TextField(phoneNumber);
        
        TextField maxTempText = new TextField(valueOf(MAX));
        TextField minTempText = new TextField(valueOf(MIN));
        TextField maxMessage = new TextField(maxText);
        TextField minMessage = new TextField(minText);
        
        Label numLabel = new Label("Update Number Here:");
        Label ForCLabel = new Label("Temp in C");
        Label maxTempLabel = new Label("Max Temp");
        Label minTempLabel = new Label("Min Temp");
        Label maxMessageLabel = new Label("Max Temp Text");
        Label minMessageLabel = new Label("Min Temp Text");
        Label showLEDLabel = new Label("LED Switch");
        
        tempBox.setFont(new Font(30));
        
        Button numberButton = new Button();
        Button FtoC = new Button();
        Button maxTempButton = new Button();
        Button minTempButton = new Button();
        Button maxMessageButton = new Button();
        Button minMessageButton = new Button();
        Button showLEDButton = new Button();
        
        numberButton.setText("Update Phone Number");
        FtoC.setText("Switch F/C Scale");
        maxTempButton.setText("Set Max Temp");
        minTempButton.setText("Set Min Temp");
        maxMessageButton.setText("Update Max Text Message");
        minMessageButton.setText("Update Min Text Message");
        showLEDButton.setText("LED ON/OFF");

        VBox root = new VBox();
        root.getChildren().addAll(chart, numLabel, number, numberButton, ForCLabel,
                tempBox, FtoC, maxTempLabel, maxTempText, maxTempButton, minTempLabel,
                minTempText, minTempButton, maxMessageLabel, maxMessage, maxMessageButton,
                minMessageLabel, minMessage, minMessageButton, showLEDLabel, showLEDButton);
        Scene scene = new Scene(root, 300, 250);
        
        primaryStage.setScene(scene);
        primaryStage.show();
               
        play();
        
        numberButton.setOnAction((ActionEvent event) -> {
            if(number.getText().matches(numCheck)){
                phoneNumber = "+1" + number.getText();
                number.clear();
                numLabel.setText("Phone Number Updated To: " + phoneNumber);
            }
            else{
                numLabel.setText("Not A Valid Phone Number");
                number.clear();
            }
        });
        
        FtoC.setOnAction((ActionEvent event) -> {
            
            // Updates label of button with Celcius
            if (celcius == true){
                ForCLabel.setText("Temp in F");
                celcius = false;
            }
            // Updates label of button with Fahrenheit
            else{
                ForCLabel.setText("Temp in C");
                celcius = true;
            }
        });
        
        // Max temperature change button
        maxTempButton.setOnAction((ActionEvent event) -> {
            int maxInt = Integer.parseInt( maxTempText.getText() );
            
            // Checks that max is less than min
            if(maxInt > MIN){
                MAX = maxInt;
                maxTempLabel.setText("Max = " + maxInt);
            }
            else{
                maxTempLabel.setText("Max lower than min! Max = " + MAX);
                maxTempText.clear();
            }
        });
        
        // Min temperature change button
        minTempButton.setOnAction((ActionEvent event) -> {
            int minInt = Integer.parseInt(minTempText.getText());
            
            // Checks that min is less than max
            if(minInt < MAX){
                MIN = minInt;
                minTempLabel.setText("Min = " + minInt);
            }
            else{
                minTempLabel.setText("Min higher than Max! Min = " + MIN);
                minTempText.clear();
            }
        });
        
        // Changes  label
        maxMessageButton.setOnAction((ActionEvent event) -> {
            maxText = maxMessage.getText();
            maxMessageLabel.setText("Message updated to: " + maxText);
        });
        
        // Changes label
        minMessageButton.setOnAction((ActionEvent event) -> {
            minText = minMessage.getText();
            minMessageLabel.setText("Message updated to: " + minText);
        });
        
        // Action listener for LED button
        showLEDButton.setOnAction((ActionEvent event) -> {
            //LED LOGIC
            if (buttonFlag == false){
                showLEDLabel.setText("LED On");
                buttonFlag = true;
                arduino.serialWrite("on");
            }
            else {
                showLEDLabel.setText("LED Off");
                buttonFlag = false;
                arduino.serialWrite("off");
            }
        });
        
        
    }
    
    public static void ledButton(Arduino arduino){
        
    }
    
    // Send message 
    public static void sendMessage(String toPhoneNumber, String body, double temperature){
    Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    Message message = Message
            .creator(new PhoneNumber(toPhoneNumber),  // to
                    new PhoneNumber("+16175448459"),  // from
                    body + " current temperature " + temperature + " C")
            
            .create();
    }

    public static void main(String[] args) {
        launch(args);
    }

}