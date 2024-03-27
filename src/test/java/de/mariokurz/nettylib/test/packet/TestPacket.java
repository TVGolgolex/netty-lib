package de.mariokurz.nettylib.test.packet;

/*
 * MIT License
 *
 * Copyright (c) 2024 17:34 Mario Pascal K.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.google.gson.JsonObject;
import de.golgolex.quala.json.JsonUtils;
import de.golgolex.quala.json.document.JsonDocument;
import de.mariokurz.nettylib.network.protocol.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@Getter
@AllArgsConstructor
public class TestPacket extends Packet implements Serializable {

    private String message;
    private JsonDocument jsonDocument;

    /**
     * Custom method for serializing the object.
     *
     * @param out The output object stream to which the object will be written.
     * @throws IOException If an error occurs while writing to the stream.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Convert the JsonObject to a JSON string and write it to the output object stream
        out.writeObject(jsonDocument.jsonObject().toString());
    }

    /**
     * Custom method for deserializing the object.
     *
     * @param in The input object stream from which the object will be read.
     * @throws IOException            If an error occurs while reading from the stream.
     * @throws ClassNotFoundException If the class of the object cannot be found.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Read the JSON string from the input object stream
        String jsonString = (String) in.readObject();
        // Convert the JSON string back to a JsonObject
        var jsonObject = JsonUtils.JSON.fromJson(jsonString, JsonObject.class);
        // Set the JsonDocument object with the deserialized JsonObject
        this.jsonDocument = new JsonDocument(jsonObject);
    }

}
