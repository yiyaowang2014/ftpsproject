/*
 * (c) 2003-2016 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ftps;

import org.mule.DefaultMuleMessage;
import org.mule.transport.AbstractMuleMessageFactory;
import org.mule.transport.file.FileConnector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPFile;

public class FtpsMuleMessageFactory extends AbstractMuleMessageFactory
{

    private FTPSClient FTPSClient;
    private boolean streaming;
    private final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        FTPFile file = (FTPFile) transportMessage;
        if (streaming)
        {
            InputStream stream = FTPSClient.retrieveFileStream(file.getName());
            if (stream == null)
            {
                throw new IOException(MessageFormat.format("Failed to retrieve file {0}. Ftp error: {1}",
                    file.getName(), FTPSClient.getReplyCode()));
            }
            return stream;
        }
        else
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try
            {

                if (!FTPSClient.retrieveFile(file.getName(), baos))
                {
                    throw new IOException(String.format("Failed to retrieve file %s. Ftp error: %s",
                                                               file.getName(), FTPSClient.getReplyCode()));
                }

                return baos.toByteArray();
            }
            catch (IOException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new IOException(String.format("Failed to retrieve file %s due to unexpected exception", file.getName()), e);
            }
            catch (OutOfMemoryError e)
            {
                throw new IOException(String.format("Failed to retrieve file %s because it's larger than the current memory heap. " +
                                                    "Consider enabling streaming on the FTP connector", file.getName()), e);
            }
        }
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[]{FTPFile.class};
    }

    @Override
    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        super.addProperties(message, transportMessage);
        
        FTPFile file = (FTPFile) transportMessage;
        message.setInboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME, file.getName());
        message.setInboundProperty(FileConnector.PROPERTY_FILE_SIZE, file.getSize());
        message.setInboundProperty(FileConnector.PROPERTY_FILE_TIMESTAMP, file.getTimestamp());
    }

    @Override
    protected String getMimeType(Object transportMessage)
    {
        if (transportMessage instanceof FTPFile)
        {
            FTPFile file = (FTPFile) transportMessage;

            return mimetypesFileTypeMap.getContentType(file.getName().toLowerCase());
        }
        else
        {
            return null;
        }
    }

    public void setFTPSClient(FTPSClient FTPSClient)
    {
        this.FTPSClient = FTPSClient;
    }

    public void setStreaming(boolean streaming)
    {
        this.streaming = streaming;
    }

}
