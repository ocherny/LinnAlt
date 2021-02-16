package com.tennington.bwextensions.roger_linn_design;

import com.bitwig.extension.api.PlatformType;
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList;
import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.ControllerExtensionDefinition;
import com.bitwig.extension.controller.api.ControllerHost;

import java.util.UUID;

public class LinnStrumentAltDefinition extends ControllerExtensionDefinition
{
   @Override
   public String getHardwareVendor()
   {
      return "Roger Linn Design";
   }

   @Override
   public String getHardwareModel()
   {
      return "LinnStrument (alternative)";
   }

   @Override
   public int getNumMidiInPorts()
   {
      return 1;
   }

   @Override
   public int getNumMidiOutPorts()
   {
      return 1;
   }

   @Override
   public void listAutoDetectionMidiPortNames(
      final AutoDetectionMidiPortNamesList list, final PlatformType platformType)
   {
      list.add(new String[] {"LinnStrument MIDI"}, new String[] {"LinnStrument MIDI"});
   }

   @Override
   public String getHelpFilePath()
   {
      return "Controllers/Roger Linn Design/LinnStrument.pdf";
   }

   @Override
   public ControllerExtension createInstance(final ControllerHost host)
   {
      return new LinnStrumentAlt(this, host);
   }

   @Override
   public String getName()
   {
      return getHardwareModel();
   }

   @Override
   public String getAuthor()
   {
      return "OlenaC";
   }

   @Override
   public String getVersion()
   {
      return "0.1";
   }

   @Override
   public UUID getId()
   {
      return EXTENSION_UUID;
   }

   @Override
   public int getRequiredAPIVersion()
   {
      return 7;
   }

   public static LinnStrumentAltDefinition getInstance()
   {
      return INSTANCE;
   }

   private static final LinnStrumentAltDefinition
      INSTANCE = new LinnStrumentAltDefinition();

   private static final UUID EXTENSION_UUID = UUID.fromString("4ba60628-6fc9-11eb-9439-0242ac130002");
}
