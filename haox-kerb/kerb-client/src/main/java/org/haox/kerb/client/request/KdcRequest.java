package org.haox.kerb.client.request;

import org.haox.asn1.type.Asn1Type;
import org.haox.kerb.client.KrbContext;
import org.haox.kerb.client.KrbOption;
import org.haox.kerb.client.KrbOptions;
import org.haox.kerb.client.preauth.PreauthContext;
import org.haox.kerb.client.preauth.PreauthHandler;
import org.haox.kerb.crypto.EncryptionHandler;
import org.haox.kerb.spec.KrbException;
import org.haox.kerb.spec.type.KerberosTime;
import org.haox.kerb.spec.type.common.*;
import org.haox.kerb.spec.type.kdc.KdcOptions;
import org.haox.kerb.spec.type.kdc.KdcRep;
import org.haox.kerb.spec.type.kdc.KdcReq;
import org.haox.kerb.spec.type.kdc.KdcReqBody;
import org.haox.kerb.spec.type.pa.PaData;
import org.haox.kerb.spec.type.pa.PaDataType;
import org.haox.transport.Transport;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * A wrapper for KdcReq request
 */
public abstract class KdcRequest {
    private KrbContext context;
    private Transport transport;

    private KrbOptions krbOptions;
    private PrincipalName serverPrincipal;
    private List<HostAddress> hostAddresses = new ArrayList<HostAddress>();
    private KdcOptions kdcOptions = new KdcOptions();
    private boolean preauthRequired = true;
    private List<EncryptionType> encryptionTypes;
    private EncryptionType chosenEncryptionType;
    private int chosenNonce;
    private KdcReq kdcReq;
    private KdcRep kdcRep;
    protected Map<String, Object> credCache;
    protected PaDataType selectedPreauthType;
    protected PaDataType allowedPreauthType;
    private PreauthHandler preauthHandler;
    private PaData preauthData;
    private boolean isRetrying;

    public KdcRequest(KrbContext context) {
        this.context = context;
        this.isRetrying = false;
        this.credCache = new HashMap<String, Object>();
        this.selectedPreauthType = PaDataType.NONE;
        this.allowedPreauthType = PaDataType.NONE;
    }

    public void setPreauthHandler(PreauthHandler preauthHandler) {
        this.preauthHandler = preauthHandler;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public Transport getTransport() {
        return this.transport;
    }

    public void setKrbOptions(KrbOptions options) {
        this.krbOptions = options;
    }

    public KrbOptions getKrbOptions() {
        return krbOptions;
    }

    protected abstract PreauthContext getPreauthContext();

    protected void loadCredCache() {
        // TODO
    }

    protected PaData getPreauthData() throws KrbException {
        return preauthData;
    }

    public KdcReq getKdcReq() {
        return kdcReq;
    }

    public void setKdcReq(KdcReq kdcReq) {
        this.kdcReq = kdcReq;
    }

    public KdcRep getKdcRep() {
        return kdcRep;
    }

    public void setKdcRep(KdcRep kdcRep) {
        this.kdcRep = kdcRep;
    }

    protected KdcReqBody makeReqBody() throws KrbException {
        KdcReqBody body = new KdcReqBody();

        long startTime = System.currentTimeMillis();
        body.setFrom(new KerberosTime(startTime));

        PrincipalName cName = null;
        cName = getClientPrincipal();
        body.setCname(cName);

        body.setRealm(cName.getRealm());

        PrincipalName sName = getServerPrincipal();
        body.setSname(sName);

        body.setTill(new KerberosTime(startTime + getTicketValidTime()));

        int nonce = generateNonce();
        body.setNonce(nonce);
        setChosenNonce(nonce);

        body.setKdcOptions(getKdcOptions());

        HostAddresses addresses = getHostAddresses();
        if (addresses != null) {
            body.setAddresses(addresses);
        }

        body.setEtypes(getEncryptionTypes());

        return body;
    }

    public KdcOptions getKdcOptions() {
        return kdcOptions;
    }

    public HostAddresses getHostAddresses() {
        HostAddresses addresses = null;
        if (!hostAddresses.isEmpty()) {
            addresses = new HostAddresses();
            for(HostAddress ha : hostAddresses) {
                addresses.addElement(ha);
            }
        }
        return addresses;
    }

    public KrbContext getContext() {
        return context;
    }

    protected byte[] decryptWithClientKey(EncryptedData data, KeyUsage usage) throws KrbException {
        return EncryptionHandler.decrypt(data, getClientKey(), usage);
    }

    public void setContext(KrbContext context) {
        this.context = context;
    }

    public void setHostAddresses(List<HostAddress> hostAddresses) {
        this.hostAddresses = hostAddresses;
    }

    public void setKdcOptions(KdcOptions kdcOptions) {
        this.kdcOptions = kdcOptions;
    }

    public abstract PrincipalName getClientPrincipal();

    public PrincipalName getServerPrincipal() {
        return serverPrincipal;
    }

    public void setServerPrincipal(PrincipalName serverPrincipal) {
        this.serverPrincipal = serverPrincipal;
    }

    public boolean isPreauthRequired() {
        return preauthRequired;
    }

    public void setPreauthRequired(boolean preauthRequired) {
        this.preauthRequired = preauthRequired;
    }

    public List<EncryptionType> getEncryptionTypes() {
        if (encryptionTypes == null) {
            encryptionTypes = context.getConfig().getEncryptionTypes();
        }
        return encryptionTypes;
    }

    public void setEncryptionTypes(List<EncryptionType> encryptionTypes) {
        this.encryptionTypes = encryptionTypes;
    }

    public EncryptionType getChosenEncryptionType() {
        return chosenEncryptionType;
    }

    public void setChosenEncryptionType(EncryptionType chosenEncryptionType) {
        this.chosenEncryptionType = chosenEncryptionType;
    }

    public int generateNonce() {
        return context.generateNonce();
    }

    public int getChosenNonce() {
        return chosenNonce;
    }

    public void setChosenNonce(int nonce) {
        this.chosenNonce = nonce;
    }

    public abstract EncryptionKey getClientKey() throws KrbException;

    public long getTicketValidTime() {
        return context.getTicketValidTime();
    }

    public KerberosTime getTicketTillTime() {
        long now = System.currentTimeMillis();
        return new KerberosTime(now + KerberosTime.MINUTE * 60 * 1000);
    }

    public void addHost(String hostNameOrIpAddress) throws UnknownHostException {
        InetAddress address = InetAddress.getByName(hostNameOrIpAddress);
        hostAddresses.add(new HostAddress(address));
    }

    public void process() throws KrbException {
        preauth();
    }

    public abstract void processResponse(KdcRep kdcRep) throws KrbException;

    protected KrbOptions getPreauthOptions() {
        return new KrbOptions();
    }

    protected void preauth() throws KrbException {
        loadCredCache();
        preauthData = new PaData();

        List<EncryptionType> etypes = getEncryptionTypes();
        if (etypes.isEmpty()) {
            throw new KrbException("No encryption type is configured and available");
        }
        EncryptionType encryptionType = etypes.iterator().next();
        setChosenEncryptionType(encryptionType);

        if (!isPreauthRequired()) {
            return;
        }

        preauthHandler.setPreauthOptions(getPreauthOptions());

        if (!isRetrying) {
            preauthHandler.tryFirst(getPreauthContext(), preauthData);
        } else {
            preauthHandler.tryAgain(getPreauthContext(), preauthData);
        }
    }

    protected String askFor(String question, String challenge) {
        return null;
    }
}
