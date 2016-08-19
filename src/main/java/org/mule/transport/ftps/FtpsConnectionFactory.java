/*
 * (c) 2003-2016 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ftps;

import org.mule.api.endpoint.EndpointURI;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.pool.PoolableObjectFactory;

public class FtpsConnectionFactory implements PoolableObjectFactory
{
    private EndpointURI uri;
    private int connectionTimeout = 0;

    public FtpsConnectionFactory(EndpointURI uri)
    {
        this.uri = uri;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    public Object makeObject() throws Exception
    {
        FTPSClient client = createFTPSClient();

        if (uri.getPort() > 0)
        {
            client.connect(uri.getHost(), uri.getPort());
        }
        else
        {
            client.connect(uri.getHost());
        }
        if (!FTPReply.isPositiveCompletion(client.getReplyCode()))
        {
            throw new IOException("Ftp connect failed: " + client.getReplyCode());
        }
        if (!client.login(uri.getUser(), uri.getPassword()))
        {
            throw new IOException("Ftp login failed: " + client.getReplyCode());
        }
        if (!client.setFileType(FTP.BINARY_FILE_TYPE))
        {
            throw new IOException("Ftp error. Couldn't set BINARY transfer type: " + client.getReplyCode());
        }
        return client;
    }

    protected FTPSClient createFTPSClient() throws NoSuchAlgorithmException
    {
        FTPSClient FTPSClient = new FTPSClient();
        FTPSClient.setConnectTimeout(connectionTimeout);

        return FTPSClient;
    }

    public void destroyObject(Object obj) throws Exception
    {
        FTPSClient client = (FTPSClient) obj;
        client.logout();
        client.disconnect();
    }

    public boolean validateObject(Object obj)
    {
        FTPSClient client = (FTPSClient) obj;
        try
        {
            return client.sendNoOp();
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public void activateObject(Object obj) throws Exception
    {
        // nothing to do
    }

    public void passivateObject(Object obj) throws Exception
    {
        // nothing to do
    }
}

