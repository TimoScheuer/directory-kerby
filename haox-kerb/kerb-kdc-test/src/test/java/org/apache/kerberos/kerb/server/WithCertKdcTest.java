package org.apache.kerberos.kerb.server;

import org.apache.kerberos.kerb.KrbException;
import org.apache.kerberos.kerb.spec.ticket.ServiceTicket;
import org.apache.kerberos.kerb.spec.ticket.TgtTicket;
import org.haox.pki.Pkix;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

/**
 openssl genrsa -out cakey.pem 2048
 openssl req -key cakey.pem -new -x509 -out cacert.pem -days 3650
 vi extensions.kdc
 openssl genrsa -out kdckey.pem 2048
 openssl req -new -out kdc.req -key kdckey.pem
 env REALM=SH.INTEL.COM openssl x509 -req -in kdc.req -CAkey cakey.pem \
 -CA cacert.pem -out kdc.pem -days 365 -extfile extensions.kdc -extensions kdc_cert -CAcreateserial
 */
public class WithCertKdcTest extends KdcTestBase {

    private Certificate userCert;
    private PrivateKey userKey;

    @Override
    protected void setUpClient() throws Exception {
        super.setUpClient();

        loadCredentials();
    }

    @Override
    protected void setUpKdcServer() throws Exception {
        super.setUpKdcServer();
        kdcServer.createPrincipals(clientPrincipal);
    }

    //@Test
    public void testKdc() throws Exception {
        Assert.assertNotNull(userCert);

        kdcServer.start();
        Assert.assertTrue(kdcServer.isStarted());
        krbClnt.init();

        TgtTicket tgt = null;
        try {
            tgt = krbClnt.requestTgtTicket(clientPrincipal, userCert, userKey, null);
        } catch (KrbException te) {
            Assert.assertTrue(te.getMessage().contains("timeout"));
            return;
        }
        Assert.assertNull(tgt);

        ServiceTicket tkt = krbClnt.requestServiceTicket(tgt, serverPrincipal, null);
        Assert.assertNull(tkt);
    }

    private void loadCredentials() throws IOException, GeneralSecurityException {
        InputStream res = getClass().getResourceAsStream("/usercert.pem");
        userCert = Pkix.getCerts(res).iterator().next();

        res = getClass().getResourceAsStream("/userkey.pem");
        userKey = Pkix.getPrivateKey(res, null);
    }
}