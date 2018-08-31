package anhtd.com.subscriber;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public abstract class SwitchCallBack implements MqttCallback {
	@Override
	public void connectionLost(Throwable cause) {
		System.out.println("MQTT subscribe connection lost. Check state of machine . Reconnectiong .....");
		this.reconnect();
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String payload = new String(message.getPayload());
		this.doWork(topic, payload);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {

	}

	public abstract void doWork(String topic, String payload);

	public abstract void reconnect();

}
