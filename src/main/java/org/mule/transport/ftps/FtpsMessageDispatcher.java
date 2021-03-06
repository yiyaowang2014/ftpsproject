/*
 * (c) 2003-2016 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ftps;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.retry.RetryContext;
import org.mule.api.transport.OutputHandler;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPSClient;

public class FtpsMessageDispatcher extends AbstractMessageDispatcher
{
    protected final FtpsConnector connector;

    public FtpsMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (FtpsConnector) endpoint.getConnector();
    }

    protected void doDispose()
    {
        // no op
    }

    protected void doDispatch(MuleEvent event) throws Exception
    {
        Object data = event.getMessage().getPayload();
        OutputStream out = connector.getOutputStream(getEndpoint(), event);

        try
        {
            if (data instanceof InputStream)
            {
                InputStream is = ((InputStream) data);
                IOUtils.copy(is, out);
                is.close();
            }
            else if (data instanceof OutputHandler)
            {
                ((OutputHandler) data).write(event, out);
            }           
            else
            {
                byte[] dataBytes;
                if (data instanceof byte[])
                {
                    dataBytes = (byte[]) data;
                }
                else
                {
                    dataBytes = data.toString().getBytes(event.getEncoding());
                }
                IOUtils.write(dataBytes, out);
            }
        }
        finally
        {
            out.close();
        }
    }

    protected MuleMessage doSend(MuleEvent event) throws Exception
    {
        doDispatch(event);
        return new DefaultMuleMessage(NullPayload.getInstance(), getEndpoint().getMuleContext());
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        try
        {
            EndpointURI uri = endpoint.getEndpointURI();
            FTPSClient client = connector.getFtp(uri);
            connector.destroyFtp(uri, client);
        }
        catch (Exception e)
        {
            // pool may be closed
        }
    }

    @Override
    public RetryContext validateConnection(RetryContext retryContext)
    {
        FTPSClient client = null;
        try
        {
            client = connector.createFTPSClient(endpoint);
            client.sendNoOp();
            client.logout();
            client.disconnect();

            retryContext.setOk();
        }
        catch (Exception ex)
        {
            retryContext.setFailed(ex);
        }
        finally
        {
            try
            {
                if (client != null)
                {
                    connector.releaseFtp(endpoint.getEndpointURI(), client);
                }
            }
            catch (Exception e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Failed to release ftp client " + client, e);
                }

            }
        }

        return retryContext;
    }
}
