package anhtd.com.subscriber;

import jota.IotaAPI;
import jota.dto.response.SendTransferResponse;
import jota.error.ArgumentException;
import jota.model.Input;
import jota.model.Transfer;
import jota.utils.Converter;
import jota.utils.TrytesConverter;

import java.util.ArrayList;
import java.util.List;

public class TestTransaction {
    private static volatile long fund = 0;
    private static volatile String payingAddress = "LDSUWOBCOBNHGKKO9YACKJDZLYCGHZZWVGWEXMZGUUU9NFHOMRW9WJRMGFWDJCMBDYYMCVXQCIHTBVQLZFYXG9YPR9";
    private final static String seed = "HQLTQBGDBXBILCLOUJ9YCKBBDNVSZKJLIRDIVAALYWXNHQ9ZHEROSNPKNDPBQRKSCESDHMDFYINANFC9L";
    private final static String tag = "MACHINE9PAYMENT999999999999";
    private final static int minWeightMagnitude = 14;
    private final static int depth = 3;



    public static void sendTransfer (final IotaAPI api, String address, String machineName, int addrIndex) {
        try {
            String refundMessage = "Refund from " + machineName + ". We are sorry sorry about the bad experience";
            Transfer refundTran = new Transfer(payingAddress, fund, TrytesConverter.toTrytes(refundMessage), tag);
            List<Transfer> refundTrans = new ArrayList<>();
            refundTrans.add(refundTran);
            List<Input> refundInput = api.getInputs(seed,2, addrIndex, addrIndex + 1, 0 ).getInputs();
            SendTransferResponse response = api.sendTransfer(seed, 2, depth, minWeightMagnitude, refundTrans, refundInput, address, false, false);
            if (response.getSuccessfully()[0]) {
                System.out.println("The transaction has been broadcasted");
            } else {
                System.out.println("The transaction has not been broadcasted");
            }
        } catch (ArgumentException e) {
            e.printStackTrace();
            System.out.println("");
        }

    }

    public static void main(String[] args) {
        String protocol = "https";
        String host = /*"mainnet.deviota.com"; "mynode.dontexist.net"*/ "walletservice.iota.community";
        String port = "443";
        final IotaAPI api = new IotaAPI.Builder().protocol(protocol).host(host).port(port).build();
        final String machineName = "[NUCE My last project] machine";
        String address = "FVSBFIZCLP9VXSXG9QHMENYLSRDIQSDYYYJJJEUIIEWEDKBENKFLSDJMYDVLDUQGVVK9PWPEWJBNFBIPYVCSEBAWGD";
        int addrIndex = 5;
        System.out.println("Test");
        Thread thread = new Thread(() -> sendTransfer(api,address,machineName,addrIndex));
        thread.start();

    }
}
