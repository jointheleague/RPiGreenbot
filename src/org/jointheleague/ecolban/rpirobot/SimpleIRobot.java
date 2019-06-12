package org.jointheleague.ecolban.rpirobot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A final class that provides a high level interface to the iRobot series of
 * Roomba/Create robots.
 * <p>
 * The names of most of the sensors are derived mostly from the <a href=
 * "https://cdn-shop.adafruit.com/datasheets/create_2_Open_Interface_Spec.pdf" >
 * iRobot Create 2 Open Interface (OI)</a>. It is recommended to read this
 * document in order to get a better understanding of how to work with the
 * robot.
 * <p>
 * The default constructor will not return until it has managed to connect with
 * the iRobot via the default serial interface between the computer and the
 * iRobot. This means that when the constructor returns, messages can be sent
 * and received to and from the iRobot.
 * <p>
 * <b>NOTE:</b> This class is final; it cannot be extended. Extend
 * {@link IRobotAdapter IRobotAdapter} instead.
 * 
 * @author Erik Colban &copy; 2016
 */
public final class SimpleIRobot implements IRobotInterface {

	/**
	 * This command puts the OI into Safe mode, enabling user control of iRobot.
	 * It turns off all LEDs. The OI can be in Passive, Safe, or Full mode to
	 * accept this command.
	 * 
	 * @see #safe()
	 */
	private static final int COMMAND_MODE_SAFE = 131;
	/**
	 * This command gives you complete control over iRobot by putting the OI
	 * into Full mode, and turning off the cliff, wheel-drop and internal
	 * charger safety features. That is, in Full mode, iRobot executes any
	 * command that you send it, even if the internal charger is plugged in, or
	 * the robot senses a cliff or wheel drop.
	 * 
	 * @see #full()
	 */
	private static final int COMMAND_MODE_FULL = 132;

	private static final int COMMAND_RESET = 7;

	private static final int COMMAND_STOP = 173;
	/**
	 * This command controls iRobot's drive wheels. It takes four data bytes,
	 * interpreted as two 16-bit signed values using two's complement. The first
	 * two bytes specify the average velocity of the drive wheels in millimeters
	 * per second (mm/s), with the high byte being sent first. The next two
	 * bytes specify the radius in millimeters at which iRobot will turn. The
	 * longer radii make iRobot drive straighter, while the shorter radii make
	 * iRobot turn more. The radius is measured from the center of the turning
	 * circle to the center of iRobot. A Drive command with a positive velocity
	 * and a positive radius makes iRobot drive forward while turning toward the
	 * left. A negative radius makes iRobot turn toward the right. Special cases
	 * for the radius make iRobot turn in place or drive straight, as specified
	 * below. A negative velocity makes iRobot drive backward. <br>
	 * NOTE: Internal and environmental restrictions may prevent iRobot from
	 * accurately carrying out some drive commands. For example, it may not be
	 * possible for iRobot to drive at full speed in an arc with a large radius
	 * of curvature. <br>
	 * + Available in modes: Safe or Full <br>
	 * + Changes mode to: No Change <br>
	 * + Velocity (-500 - 500 mm/s) <br>
	 * + Radius (-2000 - 2000 mm) <br>
	 * Special cases: <br>
	 * Straight = 32768 or 32767 = hex 8000 or 7FFF <br>
	 * Turn in place clockwise = hex FFFF <br>
	 * Turn in place counter-clockwise = hex 0001
	 * 
	 * @see #drive(int, int)
	 */
	private static final int COMMAND_DRIVE = 137;

	/**
	 * This command lets you control the forward and backward motion of Roomba’s
	 * main brush, side brush, and vacuum independently. Motor velocity cannot
	 * be controlled with this command, all motors will run at maximum speed
	 * when enabled. The main brush and side brush can be run in either
	 * direction. The vacuum only runs forward.
	 * 
	 * Serial sequence: [138] [Motors]
	 * <ul>
	 * <li>Available in modes: Safe or Full
	 * <li>Changes mode to: No Change
	 * <li>Bits 0-2: 0 = off, 1 = on at 100% pwm duty cycle
	 * <li>Bits 3 & 4: 0 = motor’s default direction, 1 = motor’s opposite
	 * direction. Default direction for the side brush is counterclockwise.
	 * Default direction for the main brush/flapper is inward.
	 * </ul>
	 */
	private static final int COMMAND_MOTORS = 138;

	/**
	 * This command controls the LEDs on iRobot. The state of LEDs is specified
	 * by two bits in the first data byte. The power LED is specified by two
	 * data bytes: one for the color and the other for the intensity.
	 * <p/>
	 * This command shall be followed by 3 data bytes:
	 * <p/>
	 * <ul>
	 * <li>LED bits, identifies the LEDS (bit 0 - Debris, bit 1 - Spot, bit 2 -
	 * Home, bit 3 - Check Robot)</li>
	 * <li>Power Color, (0 - 255) 0 = green, 255 = red. Intermediate values are
	 * intermediate colors (orange, yellow, etc).</li>
	 * <li>Power Intensity, (0 - 255) 0 = off, 255 = full intensity.
	 * Intermediate values are intermediate intensities.</li>
	 * </ul>
	 * 
	 * @see IRobotInterface#leds(int, int, boolean)
	 */
	private static final int COMMAND_LEDS = 139;
	/**
	 * Mask identifying the Clean the Clean button.
	 */
	private static final int CLEAN_BUTTON_ID = 1;
	/**
	 * Mask identifying the Spot LED and the Spot button.
	 */
	private static final int SPOT_BUTTON_LED_ID = 2;
	/**
	 * This command lets you specify up to sixteen songs to the OI that you can
	 * play at a later time. Each song is associated with a song number. The
	 * Play command uses the song number to identify your song selection. Each
	 * song can contain up to sixteen notes. Each note is associated with a note
	 * number that uses MIDI note definitions and a duration that is specified
	 * in fractions of a second. The number of data bytes varies, depending on
	 * the length of the song specified. A one note song is specified by four
	 * data bytes. For each additional note within a song, add two data bytes.
	 * 
	 * @see #song(int, int[])
	 * @see #song(int, int[], int, int)
	 */
	private static final int COMMAND_SONG = 140;
	/**
	 * This command lets you select a song to play from the songs added to
	 * iRobot using the Song command. You must add one or more songs to iRobot
	 * using the Song command in order for the Play command to work. Also, this
	 * command does not work if a song is already playing. Wait until a
	 * currently playing song is done before sending this command. Note that
	 * {@link IRobotInterface#SENSORS_SONG_PLAYING} can be used to check whether
	 * the iRobot is ready to accept this command.
	 * 
	 * @see #playSong(int)
	 */
	private static final int COMMAND_PLAY_SONG = 141;
	/**
	 * This command requests the OI to send a packet of sensor data bytes. There
	 * are 43 different sensor data packets. Each provides a value of a specific
	 * sensor or group of sensors.
	 * 
	 * @see #readSensors(int)
	 */
	private static final int COMMAND_SENSORS = 142;
	/**
	 * This command lets you control the forward and backward motion of iRobot's
	 * drive wheels independently. It takes four data bytes, which are
	 * interpreted as two 16-bit signed values using two's complement. The first
	 * two bytes specify the velocity of the right wheel in millimeters per
	 * second (mm/s), with the high byte sent first. The next two bytes specify
	 * the velocity of the left wheel, in the same format. A positive velocity
	 * makes that wheel drive forward, while a negative velocity makes it drive
	 * backward. <br>
	 * + Available in modes: Safe or Full <br>
	 * + Changes mode to: No Change <br>
	 * + Right wheel velocity (-500 - 500 mm/s) <br>
	 * + Left wheel velocity (-500 - 500 mm/s)
	 * 
	 * @see #driveDirect(int, int)
	 */
	private static final int COMMAND_DRIVE_DIRECT = 145;
	/**
	 * Time in ms to pause after sending a command to the iRobot.
	 */
	private static final int AFTER_COMMAND_PAUSE_TIME = 20;
	// Sensor values previously read:
	private int angle;
	private int batteryCapacity;
	private int batteryCharge;
	private int batteryTemperature;
	private boolean bumpLeft;
	private boolean bumpRight;
	private int chargingState;
	private boolean cliffFrontLeft;
	private int cliffSignalLeftFront;
	private boolean cliffFrontRight;
	private int cliffSignalRightFront;
	private boolean cliffLeft;
	private int cliffSignalLeft;
	private boolean cliffRight;
	private int cliffSignalRight;
	private int current;
	private int distance;
	private int encoderCountLeft;
	private int encoderCountRight;
	private boolean homeBaseChargerAvailable;
	private int infraredByte;
	private int infraredByteLeft;
	private int infraredByteRight;
	private boolean internalChargerAvailable;
	private boolean wheelOvercurrentLeft;
	private int lightBump;
	private int lightBumpSignalLeft;
	private int lightBumpSignalLeftFront;
	private int lightBumpSignalLeftCenter;
	private int lightBumpSignalRightCenter;
	private int lightBumpSignalRightFront;
	private int lightBumpSignalRight;
	private boolean wheelOvercurrentSideBrush;
	private boolean wheelOvercurrentMainBrush;
	private int motorCurrentLeft;
	private int motorCurrentRight;
	private int oiMode;
	private int buttons;
	private int requestedVelocityLeft;
	private int requestedRadius;
	private int requestedVelocityRight;
	private int requestedVelocity;
	private boolean wheelOvercurrentRight;
	private int songNumber;
	private boolean songPlaying;
	private boolean stasis;
	private boolean virtualWall;
	private int voltage;
	private boolean wall;
	private int wallSignal;
	private boolean wheelDropLeft;
	private boolean wheelDropRight;
	private SerialConnection serialConnection;
	private int powerLedColor;
	private int powerLedIntensity;
	private boolean isSpotLedOn;
	private Map<Integer, Integer> sensorGroupLow;
	private Map<Integer, Integer> sensorGroupHigh;

	/**
	 * Default constructor of a SimpleRobot instance.
	 * Equivalent to using <code>SimpleIRobot(false, true, true)</code>
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 * @see #SimpleIRobot(boolean, boolean, boolean)
	 */
	public SimpleIRobot() throws InterruptedException, IOException {
		this(false, true, true);
	}

	/**
	 * Constructor
	 * 
	 * @param debugSerial
	 *            if true will create a default serial connection with debug
	 *            true
	 * @param fullMode
	 *            if true enter full mode, otherwise enter safe mode
	 * @param waitButton
	 *            if true wait until play button is pressed
	 * @throws IOException
	 * @throws InterruptedException
	 * 
	 */
	public SimpleIRobot(boolean debugSerial, boolean fullMode, boolean waitButton)
			throws InterruptedException, IOException {
		this(SerialConnection.getInstance(debugSerial), fullMode, waitButton);
	}

	/**
	 * Constructor that uses a given serial connection as its means of sending
	 * and reading data to and from the iRobot.
	 * 
	 * @param sc
	 *            user-specified serial connection.
	 * @param fullMode
	 *            if true enter full mode, otherwise enter safe mode
	 * @param waitButton
	 *            if true wait until play button is pressed
	 * @throws IOException
	 * 
	 */
	SimpleIRobot(SerialConnection sc, boolean fullMode, boolean waitButton) throws IOException {
		this.serialConnection = sc;
		buildSensorGroups();
		if (fullMode) {
			full();
		} else {
			safe();
		}
		if (waitButton) {
			waitButtonPressed(true);
		}
		startSpotListener();
	}

	private void startSpotListener() {
		Thread spotListener = new Thread(new Runnable() {

			@Override
			public void run() {
				boolean running = true;
				while (running) {
					try {
						Thread.sleep(1000L);
					} catch (InterruptedException e) {
					}
					try {
						readSensors(SENSORS_BUTTONS);
					} catch (IOException e) {
						System.out.println(e.getMessage());
					}
					// System.out.println("Spot button sensor read.");
					if (isSpotButtonDown()) {
						// System.out.println("Spot button down.");
						try {
							stop();
						} catch (IOException e) {
							System.out.println(e.getMessage());
						}
						closeConnection();
						running = false;
					}
				}
			}
		});
		spotListener.setDaemon(true);
		spotListener.start();
		// System.out.println("Spot listener started.");
	}

	private void buildSensorGroups() {
		sensorGroupLow = new HashMap<>();
		sensorGroupHigh = new HashMap<>();
		addGroup(0, 7, 26);
		addGroup(1, 7, 16);
		addGroup(2, 17, 20);
		addGroup(3, 21, 26);
		addGroup(4, 27, 34);
		addGroup(5, 35, 42);
		addGroup(6, 7, 42);
		addGroup(100, 7, 58);
		addGroup(101, 43, 58);
		addGroup(106, 46, 51);
		addGroup(107, 54, 58);
	}

	private void addGroup(int key, int low, int high) {
		sensorGroupLow.put(key, low);
		sensorGroupHigh.put(key, high);

	}

	@Override
	public synchronized void drive(int velocity, int radius) throws IOException {
		serialConnection.writeByte(COMMAND_DRIVE);
		serialConnection.writeSignedWord(velocity);
		serialConnection.writeSignedWord(radius);
		try {
			Thread.sleep(AFTER_COMMAND_PAUSE_TIME);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public synchronized void driveDirect(int leftVelocity, int rightVelocity) throws IOException {
		serialConnection.writeByte(COMMAND_DRIVE_DIRECT);
		serialConnection.writeSignedWord(rightVelocity);
		serialConnection.writeSignedWord(leftVelocity);
		try {
			Thread.sleep(AFTER_COMMAND_PAUSE_TIME);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public synchronized void full() throws IOException {
		serialConnection.writeByte(COMMAND_MODE_FULL);
		try {
			Thread.sleep(AFTER_COMMAND_PAUSE_TIME);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public synchronized int getAngle() {
		return angle;
	}

	@Override
	public synchronized int getBatteryCapacity() {
		return batteryCapacity;
	}

	@Override
	public synchronized int getBatteryCharge() {
		return batteryCharge;
	}

	@Override
	public synchronized int getBatteryTemperature() {
		return batteryTemperature;
	}

	@Override
	public synchronized int getChargingState() {
		return chargingState;
	}

	@Override
	public synchronized int getCliffSignalLeftFront() {
		return cliffSignalLeftFront;
	}

	@Override
	public synchronized int getCliffSignalRightFront() {
		return cliffSignalRightFront;
	}

	@Override
	public synchronized int getCliffSignalLeft() {
		return cliffSignalLeft;
	}

	@Override
	public synchronized int getCliffSignalRight() {
		return cliffSignalRight;
	}

	@Override
	public synchronized int getCurrent() {
		return current;
	}

	@Override
	public synchronized int getDistance() {
		return distance;
	}

	@Override
	public int getEncoderCountLeft() {
		return encoderCountLeft;
	}

	@Override
	public int getEncoderCountRight() {
		return encoderCountRight;
	}

	@Override
	public synchronized int getInfraredByte() {
		return infraredByte;
	}

	@Override
	public int getInfraredByteLeft() {
		return infraredByteLeft;
	}

	@Override
	public int getInfraredByteRight() {
		return infraredByteRight;
	}

	@Override
	public synchronized int getOiMode() {
		return oiMode;
	}

	@Override
	public synchronized int getRequestedVelocityLeft() {
		return requestedVelocityLeft;
	}

	@Override
	public synchronized int getRequestedRadius() {
		return requestedRadius;
	}

	@Override
	public synchronized int getRequestedVelocityRight() {
		return requestedVelocityRight;
	}

	@Override
	public synchronized int getRequestedVelocity() {
		return requestedVelocity;
	}

	// private boolean getSensorBoolean(int sensorId) {
	// return sensorValues[sensorId] != 0;
	// }
	//
	// private int getSensorInteger(int sensorId) {
	// return sensorValues[sensorId];
	// }

	@Override
	public synchronized int getSongNumber() {
		return songNumber;
	}

	@Override
	public synchronized int getVoltage() {
		return voltage;
	}

	@Override
	public int getMotorCurrentLeft() {
		return motorCurrentLeft;
	}

	@Override
	public int getMotorCurrentRight() {
		return motorCurrentRight;
	}

	@Override
	public boolean isStasis() {
		return stasis;
	}

	@Override
	public synchronized int getWallSignal() {
		return wallSignal;
	}

	@Override
	public synchronized boolean isBumpLeft() {
		return bumpLeft;
	}

	@Override
	public synchronized boolean isBumpRight() {
		return bumpRight;
	}

	@Override
	public boolean isLightBump() {
		return (lightBump & 0x3F) != 0;
	}

	@Override
	public int[] getLightBumps() {
		int[] result = new int[6];
		int bumps = lightBump;
		result[0] = (bumps & 0x1) != 0 ? lightBumpSignalLeft : 0;
		result[1] = (bumps & 0x2) != 0 ? lightBumpSignalLeftFront : 0;
		result[2] = (bumps & 0x4) != 0 ? lightBumpSignalLeftCenter : 0;
		result[3] = (bumps & 0x8) != 0 ? lightBumpSignalRightCenter : 0;
		result[4] = (bumps & 0x10) != 0 ? lightBumpSignalRightFront : 0;
		result[5] = (bumps & 0x20) != 0 ? lightBumpSignalRight : 0;
		return result;
	}

	@Override
	public synchronized boolean isCleanButtonDown() {
		return (buttons & CLEAN_BUTTON_ID) != 0;
	}

	@Override
	public synchronized boolean isCliffFrontLeft() {
		return cliffFrontLeft;
	}

	@Override
	public synchronized boolean isCliffFrontRight() {
		return cliffFrontRight;
	}

	@Override
	public synchronized boolean isCliffLeft() {
		return cliffLeft;
	}

	@Override
	public synchronized boolean isCliffRight() {
		return cliffRight;
	}

	@Override
	public synchronized boolean isHomeBaseChargerAvailable() {
		return homeBaseChargerAvailable;
	}

	@Override
	public synchronized boolean isInternalChargerAvailable() {
		return internalChargerAvailable;
	}

	@Override
	public synchronized boolean isLeftWheelOvercurrent() {
		return wheelOvercurrentLeft;
	}

	@Override
	public synchronized boolean isWheelOvercurrentSideBrush() {
		return wheelOvercurrentSideBrush;
	}

	@Override
	public synchronized boolean isWheelOvercurrentMainBrush() {
		return wheelOvercurrentMainBrush;
	}

	@Override
	public synchronized boolean isRightWheelOvercurrent() {
		return wheelOvercurrentRight;
	}

	@Override
	public synchronized boolean isSongPlaying() {
		return songPlaying;
	}

	@Override
	public synchronized boolean isSpotButtonDown() {
		return (buttons & SPOT_BUTTON_LED_ID) != 0;
	}

	@Override
	public synchronized boolean isVirtualWall() {
		return virtualWall;
	}

	@Override
	public synchronized boolean isWall() {
		return wall;
	}

	@Override
	public synchronized boolean isWheelDropLeft() {
		return wheelDropLeft;
	}

	@Override
	public synchronized boolean isWheelDropRight() {
		return wheelDropRight;
	}

	@Override
	public synchronized void leds(int powerColor, int powerIntensity, boolean spotLedOn) throws IOException {
		serialConnection.writeByte(COMMAND_LEDS);
		serialConnection.writeByte(spotLedOn ? SPOT_BUTTON_LED_ID : 0);
		serialConnection.writeByte(powerColor);
		serialConnection.writeByte(powerIntensity);
		try {
			Thread.sleep(AFTER_COMMAND_PAUSE_TIME);
		} catch (InterruptedException e) {
		}
		powerLedColor = powerColor;
		powerLedIntensity = powerIntensity;
		isSpotLedOn = spotLedOn;
	}

	@Override
	public synchronized void ledsToggle(boolean togglePower) throws IOException {
		if (togglePower) {
			powerLedIntensity = powerLedIntensity ^ 0xFF;
		}
		isSpotLedOn = !isSpotLedOn;
		leds(powerLedColor, powerLedIntensity, isSpotLedOn);
	}

	@Override
	public synchronized void playSong(int songNumber) throws IOException {
		serialConnection.writeByte(COMMAND_PLAY_SONG);
		serialConnection.writeByte(songNumber);
		try {
			Thread.sleep(AFTER_COMMAND_PAUSE_TIME);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public synchronized void readSensors(int sensorId) throws IOException {
		serialConnection.writeByte(COMMAND_SENSORS);
		serialConnection.writeByte(sensorId);
		try {
			Thread.sleep(AFTER_COMMAND_PAUSE_TIME);
		} catch (InterruptedException e) {
		}
		saveSensorData(sensorId);
	}

	private void saveSensorData(int sensorId) throws IOException {
		int low = sensorId;
		if (sensorGroupLow.containsKey(sensorId)) {
			low = sensorGroupLow.get(sensorId);
		}
		int high = sensorId;
		if (sensorGroupHigh.containsKey(sensorId)) {
			high = sensorGroupHigh.get(sensorId);
		}
		for (int i = low; i <= high; i++) {
			saveSensorDataPrim(i);
		}
	}

	private void saveSensorDataPrim(int sensorId) throws IOException {
		int dataByte, dataWord;

		switch (sensorId) {
		case SENSORS_BUMPS_AND_WHEEL_DROPS:
			dataByte = serialConnection.readUnsignedByte();
			bumpRight = (dataByte & 0x01) != 0;
			bumpLeft = (dataByte & 0x02) != 0;
			wheelDropRight = (dataByte & 0x04) != 0;
			wheelDropLeft = (dataByte & 0x08) != 0;
			break;
		case SENSORS_WALL:
			dataByte = serialConnection.readUnsignedByte();
			wall = (dataByte & 0x01) != 0;
			break;
		case SENSORS_CLIFF_LEFT:
			dataByte = serialConnection.readUnsignedByte();
			cliffLeft = (dataByte & 0x01) != 0;
			break;
		case SENSORS_CLIFF_FRONT_LEFT:
			dataByte = serialConnection.readUnsignedByte();
			cliffFrontLeft = (dataByte & 0x01) != 0;
			break;
		case SENSORS_CLIFF_FRONT_RIGHT:
			dataByte = serialConnection.readUnsignedByte();
			cliffFrontRight = (dataByte & 0x01) != 0;
			break;
		case SENSORS_CLIFF_RIGHT:
			dataByte = serialConnection.readUnsignedByte();
			cliffRight = (dataByte & 0x01) != 0;
			break;
		case SENSORS_VIRTUAL_WALL:
			dataByte = serialConnection.readUnsignedByte();
			virtualWall = (dataByte & 0x01) != 0;
			break;
		case SENSORS_WHEEL_OVERCURRENTS:
			dataByte = serialConnection.readUnsignedByte();
			wheelOvercurrentSideBrush = (dataByte & 0x01) != 0;
			wheelOvercurrentMainBrush = (dataByte & 0x04) != 0;
			wheelOvercurrentRight = (dataByte & 0x08) != 0;
			wheelOvercurrentLeft = (dataByte & 0x10) != 0;
			break;
		case SENSORS_DIRT_DETECT:
			serialConnection.readUnsignedByte();
			break;
		case SENSORS_DUMMY2:
			serialConnection.readUnsignedByte();
			break;
		case SENSORS_INFRARED_BYTE:
			infraredByte = serialConnection.readUnsignedByte();
			break;
		case SENSORS_INFRARED_BYTE_LEFT:
			infraredByteLeft = serialConnection.readUnsignedByte();
			break;
		case SENSORS_INFRARED_BYTE_RIGHT:
			infraredByteRight = serialConnection.readUnsignedByte();
			break;
		case SENSORS_BUTTONS:
			dataByte = serialConnection.readUnsignedByte();
			buttons = dataByte;
			break;
		case SENSORS_DISTANCE:
			dataWord = serialConnection.readSignedWord();
			distance = dataWord;
			break;
		case SENSORS_ANGLE:
			dataWord = serialConnection.readSignedWord();
			angle = dataWord;
			break;
		case SENSORS_CHARGING_STATE:
			chargingState = serialConnection.readUnsignedByte();
			break;
		case SENSORS_VOLTAGE:
			voltage = serialConnection.readUnsignedWord();
			break;
		case SENSORS_CURRENT:
			current = serialConnection.readSignedWord();
			break;
		case SENSORS_BATTERY_TEMPERATURE:
			batteryTemperature = serialConnection.readSignedByte();
			break;
		case SENSORS_BATTERY_CHARGE:
			batteryCharge = serialConnection.readUnsignedWord();
			break;
		case SENSORS_BATTERY_CAPACITY:
			batteryCapacity = serialConnection.readUnsignedWord();
			break;
		case SENSORS_WALL_SIGNAL:
			wallSignal = serialConnection.readUnsignedWord();
			break;
		case SENSORS_CLIFF_SIGNAL_LEFT:
			cliffSignalLeft = serialConnection.readUnsignedWord();
			break;
		case SENSORS_CLIFF_SIGNAL_LEFT_FRONT:
			cliffSignalLeftFront = serialConnection.readUnsignedWord();
			break;
		case SENSORS_CLIFF_SIGNAL_RIGHT_FRONT:
			cliffSignalRightFront = serialConnection.readUnsignedWord();
			break;
		case SENSORS_CLIFF_SIGNAL_RIGHT:
			cliffSignalRight = serialConnection.readUnsignedWord();
			break;
		case SENSORS_CARGO_BAY_DIGITAL_INPUTS:
			serialConnection.readUnsignedByte();
			break;
		case SENSORS_CARGO_BAY_ANALOG_SIGNAL:
			serialConnection.readUnsignedWord();
			break;
		case SENSORS_CHARGING_SOURCES_AVAILABLE:
			dataByte = serialConnection.readUnsignedByte();
			internalChargerAvailable = (dataByte & 0x01) != 0;
			homeBaseChargerAvailable = (dataByte & 0x02) != 0;
			break;
		case SENSORS_OI_MODE:
			oiMode = serialConnection.readUnsignedByte();
			break;
		case SENSORS_SONG_NUMBER:
			songNumber = serialConnection.readUnsignedByte();
			break;
		case SENSORS_SONG_PLAYING:
			dataByte = serialConnection.readUnsignedByte();
			songPlaying = (dataByte & 0x01) != 0;
			break;
		case SENSORS_NUMBER_OF_STREAM_PACKETS:
			serialConnection.readUnsignedByte();
			break;
		case SENSORS_REQUESTED_VELOCITY:
			requestedVelocity = serialConnection.readSignedWord();
			break;
		case SENSORS_REQUESTED_RADIUS:
			requestedRadius = serialConnection.readSignedWord();
			break;
		case SENSORS_REQUESTED_VELOCITY_RIGHT:
			requestedVelocityRight = serialConnection.readSignedWord();
			break;
		case SENSORS_REQUESTED_VELOCITY_LEFT:
			requestedVelocityLeft = serialConnection.readSignedWord();
			break;
		case SENSORS_ENCODER_COUNT_LEFT:
			encoderCountLeft = serialConnection.readUnsignedWord();
			break;
		case SENSORS_ENCODER_COUNT_RIGHT:
			encoderCountRight = serialConnection.readUnsignedWord();
			break;
		case SENSORS_LIGHT_BUMPER:
			lightBump = serialConnection.readUnsignedByte();
			break;
		case SENSORS_LIGHT_BUMP_SIGNAL_LEFT:
			lightBumpSignalLeft = serialConnection.readUnsignedWord();
			break;
		case SENSORS_LIGHT_BUMP_SIGNAL_LEFT_FRONT:
			lightBumpSignalLeftFront = serialConnection.readUnsignedWord();
			break;
		case SENSORS_LIGHT_BUMP_SIGNAL_LEFT_CENTER:
			lightBumpSignalLeftCenter = serialConnection.readUnsignedWord();
			break;
		case SENSORS_LIGHT_BUMP_SIGNAL_RIGHT:
			lightBumpSignalRight = serialConnection.readUnsignedWord();
			break;
		case SENSORS_LIGHT_BUMP_SIGNAL_RIGHT_FRONT:
			lightBumpSignalRightFront = serialConnection.readUnsignedWord();
			break;
		case SENSORS_LIGHT_BUMP_SIGNAL_RIGHT_CENTER:
			lightBumpSignalRightCenter = serialConnection.readUnsignedWord();
			break;
		case SENSORS_MOTOR_CURRENT_LEFT:
			motorCurrentLeft = serialConnection.readUnsignedWord();
			break;
		case SENSORS_MOTOR_CURRENT_RIGHT:
			motorCurrentRight = serialConnection.readUnsignedWord();
			break;
		case SENSORS_MAIN_BRUSH_MOTOR_CURRENT:
			serialConnection.readSignedWord();
			break;
		case SENSORS_SIDE_BRUSH_MOTOR_CURRENT:
			serialConnection.readSignedWord();
			break;
		case SENSORS_STASIS:
			dataByte = serialConnection.readUnsignedByte();
			stasis = (dataByte & 0x1) != 0;
			break;
		default:
			throw new IllegalArgumentException(String.valueOf(sensorId));
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		serialConnection.writeByte(COMMAND_RESET);
		try {
			Thread.sleep(AFTER_COMMAND_PAUSE_TIME);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public synchronized void safe() throws IOException {
		serialConnection.writeByte(COMMAND_MODE_SAFE);
		try {
			Thread.sleep(AFTER_COMMAND_PAUSE_TIME);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public synchronized void song(int songNumber, int[] notesAndDurations) throws IOException {
		song(songNumber, notesAndDurations, 0, notesAndDurations.length);
	}

	@Override
	public synchronized void song(int songNumber, int[] notesAndDurations, int startIndex, int length)
			throws IOException {
		if (songNumber < 0 || songNumber > 15) {
			throw new IllegalArgumentException("songNumber " + songNumber);
		}
		if ((length & 0x01) == 0x01) {
			throw new IllegalArgumentException("length " + songNumber + "must be even");
		}
		if (length < 1 || length > (256 - (songNumber * 16 * 2))) {
			throw new IllegalArgumentException("length " + length);
		}
		System.out.println("Writing song to Roomba");
		serialConnection.writeByte(COMMAND_SONG);
		serialConnection.writeByte(songNumber);
		serialConnection.writeByte(length >> 1);
		serialConnection.writeBytes(notesAndDurations, startIndex, length);
		try {
			Thread.sleep(AFTER_COMMAND_PAUSE_TIME);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void stop() throws IOException {
		serialConnection.writeByte(COMMAND_STOP);
		try {
			Thread.sleep(AFTER_COMMAND_PAUSE_TIME);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public synchronized void waitButtonPressed(boolean beep) throws IOException {
		int startingPowerLedIntensity = powerLedIntensity;
		int startingPowerLedColor = powerLedColor;
		boolean startingSpotLedState = isSpotLedOn;
		int totalTimeWaiting = 0;
		boolean cleanButtonPressed = false;
		boolean cleanButtonReleased = false;
		final int noteDuration = 16;
		if (beep) {
			song(0, new int[] { 58, noteDuration, 62, noteDuration });
		}
		while (!cleanButtonReleased) {
			readSensors(SENSORS_BUTTONS);
			if (cleanButtonPressed && !isCleanButtonDown()) {
				cleanButtonReleased = true;
			} else {
				if (isCleanButtonDown()) {
					cleanButtonPressed = true;
				}
				if (totalTimeWaiting > 500) {
					System.out.println("Checking beep: " + beep);
					if (beep) {
						playSong(0);
					}
					ledsToggle(true);
					totalTimeWaiting = 0;
				}
				try {
					Thread.sleep(noteDuration);
				} catch (InterruptedException e) {
				}
				totalTimeWaiting += noteDuration;
			}
		}
		leds(startingPowerLedColor, startingPowerLedIntensity, startingSpotLedState);
	}

	@Override
	public void closeConnection() {
		if (serialConnection != null) {
			serialConnection.close();
		}
	}

	@Override
	public void setTailLight(boolean on) throws IOException {
		serialConnection.writeByte(COMMAND_MOTORS);
		serialConnection.writeByte(on ? 2 : 0);
	}
}
