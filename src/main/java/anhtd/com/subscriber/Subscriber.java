package anhtd.com.subscriber;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jota.model.Input;
import jota.utils.Checksum;
import jota.utils.TrytesConverter;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import jota.*;
import jota.dto.response.GetBalancesAndFormatResponse;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Transaction;
import jota.model.Transfer;
import jota.utils.StopWatch;

public class Subscriber {

	private static volatile long fund = 0;
	private static volatile boolean isRunning = false;
	private static volatile MqttClient mqttClient;
	private static volatile String payingAddress = null;
	private final static String seed = "HQLTQBGDBXBILCLOUJ9YCKBBDNVSZKJLIRDIVAALYWXNHQ9ZHEROSNPKNDPBQRKSCESDHMDFYINANFC9L";
	private final static String tag = "IOTA9MACHINE9PAYMENT";
    private final static int minWeightMagnitude = 14;
    private final static int depth = 3;
    private static String machineName ;
    private static String refundMessage ;
    private static long pricePerSecond ;
    private static String in_topic_lamp;
    private static String out_topic_lamp;
    private static String doing_time_topic;
    private static String content;
    private static String broker;
    private static volatile int qos;
    private static String clientId;
    private static MemoryPersistence persistence;
    private static String protocol;
    private static String host;
    private static String port;
    private static IotaAPI api;
    private static String address;
    private static int addrIndex;
    private static MqttCallback switchCallBack;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                try {
                    turnOffDevice();
                    Thread.sleep(1000);
                } catch (MqttException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    public static void main(String[] args) {
        machineName = "[NUCE My last project] machine"; // will this property get from file
        refundMessage = "Refund from " + machineName + ". We are sorry about the bad experience";
        pricePerSecond = 1;
        in_topic_lamp = "bedroom/desktop-lamp-in";
        out_topic_lamp = "bedroom/desktop-lamp-out";
        doing_time_topic = "lcd/time_counter_machine_1";
        content = "Message from MqttPublish "+ machineName;
        broker = "tcp://192.168.100.5:1883";
        qos = 2;
		persistence = new MemoryPersistence();
		clientId = "balance_change_subscriber";
		protocol = "https";
		host = "walletservice.iota.community"; //"mynode.dontexist.net";
        port = "443";
        api = new IotaAPI.Builder().protocol(protocol).host(host).port(port).build();
        address = "LDSUWOBCOBNHGKKO9YACKJDZLYCGHZZWVGWEXMZGUUU9NFHOMRW9WJRMGFWDJCMBDYYMCVXQCIHTBVQLZ";
        addrIndex = 6;
        switchCallBack = new SwitchCallBack() {
			@Override
			public void doWork(String topic, String payload) {
				if (topic.equals(out_topic_lamp) && payload.equals("1")) {
					System.out.println("Machine is running");
					isRunning = true;
					if (fund > 0) {
						// set payingAddress here
					//	long startTime = System.currentTimeMillis();
//						long doingSeconds = fund / pricePerSecond;
//						//long endTime = doingSeconds * 1000 + startTime;
//						// publish thoi gian ket thuc theo doingSeconds de hien thi ra ngoai man hinh
//						// led cho nguoi thanh toan xem
//						try {
//                            publishDoingTime(doingSeconds);
//						} catch (MqttException e) {
//							e.printStackTrace();
//							System.out.println("Publish doing time message failed . May be the client LCD is showing nothing !");
//						}
						try {
							while (fund >= pricePerSecond) {
								fund = fund - pricePerSecond;
								Thread.sleep(1000);
								System.out.println("Time:" + System.currentTimeMillis() + " | " + fund);
							}
							Thread.sleep(1000);

						} catch(InterruptedException e){
							e.printStackTrace();
						}
						try {
							turnOffDevice();
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println("Publish turn off message failed . May be machine is still running ");
						}
						fund = 0;
						isRunning = false;

					}
				} else if (topic.equals(out_topic_lamp) && payload.equals("0")) {
					System.out.println("Machine turn off");
					isRunning = false;
					// if turn off when fund is still avaiavble , need refund
					if (fund > 0) {
						refund(api, address, addrIndex);
						fund = 0;
					}
				}
			}

            @Override
            public void reconnect() {
                try {
                    reconectMqtt();
                } catch (MqttException me) {
                    System.out.println("Reconnecting to MQTT broker has been failed ");
                    reconnect();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

		try {
			reconectMqtt();
			checkBalanceAndPublish();

		} catch (MqttException me) {
            System.out.println("Connecting to MQTT broker has been failed");
            System.out.println("reason " + me.getReasonCode());
			System.out.println("msg " + me.getMessage());
			System.out.println("loc " + me.getLocalizedMessage());
			System.out.println("cause " + me.getCause());
			System.out.println("excep " + me);
		} catch (NullPointerException ne) {
            System.out.println("Connecting to IOTA IRI NODE has been failed");
            System.out.println("msg " + ne.getMessage());
            System.out.println("loc " + ne.getLocalizedMessage());
            System.out.println("cause " + ne.getCause());
            System.out.println("excep " + ne);
        }

	}

	private static void checkBalanceAndPublish() {
		List<String> address1 = new ArrayList<>();
		address1.add(address);
		long balance = 0;
		try {
			GetBalancesAndFormatResponse balanceResponse = api.getBalanceAndFormat(address1, 0, 0, new StopWatch(), 2);
			balance = balanceResponse.getTotalBalance();
			System.out.println("balance check: " + balance + " | time check: " + balanceResponse.getDuration() + " | started balance ");
			//TODO for testing
			fund = fund + 15;
			for (int i = 0; i >= 0; i++) {
            	balanceResponse = api.getBalanceAndFormat(address1, 0, 0, new StopWatch(), 2);
				fund = fund + balanceResponse.getTotalBalance() - balance;
				balance = balanceResponse.getTotalBalance();
				if (fund >= pricePerSecond && !isRunning) {
                    setPayingAddress();
                    System.out.println("balance check: " + balance + " | time check: " + balanceResponse.getDuration() + " (s) | address: " + payingAddress);
                    try {
                    	isRunning = true;
						long doingSeconds = fund / pricePerSecond;
						//long endTime = doingSeconds * 1000 + startTime;
//						try {
//							publishDoingTime(doingSeconds);
//						} catch (MqttException e) {
//							e.printStackTrace();
//							System.out.println("Publish doing time message failed . May be the client LCD is showing nothing !");
//						}
						turnOnDevice(doingSeconds);

					} catch (Exception e) {
						isRunning = false;
						System.out.println("Publish turn on message failed. Refund money");
						refund(api, address, addrIndex);
					}
				} else if (fund < pricePerSecond) {
					// set fund = 0 after refund decrease current balance -> fund < 02
					fund = 0;
				}
			}
		} catch (ArgumentException e) {
			System.out.println("balance check api error");
			e.printStackTrace();
		}
	}

    public static void reconectMqtt() throws MqttException {
        mqttClient = new MqttClient(broker, clientId, persistence);
        mqttClient.setTimeToWait(3000);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        System.out.println("Connecting to broker: " + broker);
        mqttClient.connect(connOpts);
        System.out.println("Connected");
        mqttClient.subscribe(out_topic_lamp);
        mqttClient.setCallback(switchCallBack);
    }

	private static void turnOnDevice(long doingtTime) throws MqttException, MqttPersistenceException {
		byte[] payload = ("1"+doingtTime).getBytes();
		MqttMessage message = new MqttMessage();
		message.setQos(qos);
		message.setPayload(payload);
        MqttTopic mqttTopic = mqttClient.getTopic(in_topic_lamp);
        mqttTopic.publish(message);
        System.out.println("Publishing message: Turn on device ");
	}

	private static void turnOffDevice() throws MqttException, MqttPersistenceException {
		byte[] payload = "0".getBytes();
		MqttMessage message = new MqttMessage(content.getBytes());
		message.setQos(qos);
		message.setPayload(payload);
        MqttTopic mqttTopic = mqttClient.getTopic(in_topic_lamp);
        mqttTopic.publish(message);
        System.out.println("Publishing message: Turn off device ");

    }
	
	private static void publishDoingTime(Long period) throws MqttException, MqttPersistenceException {
		byte[] payload = period.toString().getBytes();
		MqttMessage message = new MqttMessage();
		message.setQos(qos);
		message.setPayload(payload);
        MqttTopic mqttTopic = mqttClient.getTopic(doing_time_topic);
        mqttTopic.publish(message);
        System.out.println("Publishing message: Display doing time ");
    }

	private static void setPayingAddress(){
			try {
				List<Transaction> transHistory = api.findTransactionObjectsByAddresses(new String[]{address});
				List<Transaction> transInBundle = api.findTransactionObjectsByBundle(new String[]{transHistory.get(transHistory.size() - 1).getBundle()});
				for (Transaction tran : transInBundle) {
					// need to check is index 0 is newest ?
					if (tran.getValue() < 0) {
						payingAddress = tran.getAddress();
						System.out.println(payingAddress + " : " + fund + "IOTA is paying");
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				payingAddress = null;
			}
	}
	
	private static void refund(final IotaAPI api, String address, int addrIndex) {
		try {
			if (payingAddress == null ) {
				List<Transaction> transHistory = api.findTransactionObjectsByAddresses(new String[]{address});
				List<Transaction> transInBundle = api.findTransactionObjectsByBundle(new String[]{transHistory.get(transHistory.size() - 1).getBundle()});
				for (Transaction tran : transInBundle) {
					if (tran.getValue() < 0) {
						payingAddress = tran.getAddress();
						break;
					}
				}
			}
            System.out.println(payingAddress + " : " + fund + "IOTA is being refunded");
            Transfer refundTran = new Transfer(Checksum.addChecksum(payingAddress), fund, TrytesConverter.toTrytes(refundMessage), tag);
			List<Transfer> refundTrans = new ArrayList<>();
			refundTrans.add(refundTran);
            List<Input> refundInput = api.getInputs(seed,2, addrIndex, addrIndex + 1, 0 ).getInputs();
			SendTransferResponse response = api.sendTransfer(seed, 2, depth, minWeightMagnitude, refundTrans, refundInput, address, false, false);
			if (response.getSuccessfully()[0]) {
				System.out.println("The transaction has been broadcasted");
			}
		} catch (Exception e) {
			if (payingAddress != null) {
				System.out.println("Error when refund money for address: " + payingAddress);
			} else {
				System.out.println("Error when refund money ");
			}
			e.printStackTrace();
		}
	}

}
