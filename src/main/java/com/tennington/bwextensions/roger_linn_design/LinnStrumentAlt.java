package com.tennington.bwextensions.roger_linn_design;

import com.bitwig.extension.controller.ControllerExtension;
import com.bitwig.extension.controller.api.*;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LinnStrumentAlt extends ControllerExtension
{
   private static final int MSG_NOTE_ON = 9;

   private static final int MSG_NOTE_OFF = 8;

   private static final int MSG_CC = 11;

   protected static final int       NOTE_OFF      = 0;
   protected static final int       NOTE_ON       = 1;
   protected static final int       NOTE_ON_NEW   = 2;

   public int[] noteCache = new int[127];

   int selectedTrack = -1;

   int channel = 1;

   public LinnStrumentAlt(final LinnStrumentAltDefinition definition, final ControllerHost host)
   {
      super(definition, host);
   }

   @Override
   public void init()
   {
      Logger.getGlobal().log(Level.INFO, "Test1");

      final ControllerHost host = getHost();

      host.println("Init");

      final MidiIn midiIn = host.getMidiInPort(0);
      final NoteInput noteInput = midiIn.createNoteInput("", "??????");

      mMidiOut = host.getMidiOutPort(0);

      noteInput.setShouldConsumeEvents(false);

      noteInput.setUseExpressiveMidi(true, 0, 24);

      int sends = 0;
      int scenes = 0;
      int tracks = 100;

      midiIn.setMidiCallback(this::callback);

      var cursorTrack = host.createCursorTrack ("MyCursorTrackID", "The Cursor Track", sends, scenes, true);
      var bank = host.createMainTrackBank (tracks, sends, scenes);
      bank.followCursorTrack(cursorTrack);

      for (int i = 0; i < tracks; i++) {
         Track track = bank.getItemAt(i);
         final int index = i;
         track.playingNotes().addValueObserver(arr -> handleNotes(index, track, arr));

      }
      bank.cursorIndex ().addValueObserver (index -> {
         selectedTrack = index;
         clearNotes();
      });


      final String[] yesNo = {"Yes", "No"};
      final SettableEnumValue shouldSendInit =
         host.getPreferences().getEnumSetting("Send initialization messages", "MPE", yesNo, "Yes");

      shouldSendInit.addValueObserver(newValue ->
      {
         mShouldSendInit = newValue.equalsIgnoreCase("Yes");

         if (mShouldSendInit && mDidRunInitTask)
         {
            sendInitializationMessages();
            sendPitchbendRange(mPitchBendRange);
         }
      });

      final SettableRangedValue bendRange =
         host.getPreferences().getNumberSetting("Pitch Bend Range", "MPE", 1, 96, 1, "", 48);

      bendRange.addRawValueObserver(range ->
      {
         mPitchBendRange = (int)range;
         noteInput.setUseExpressiveMidi(true, 0, mPitchBendRange);

         if (mShouldSendInit && mDidRunInitTask)
         {
            sendPitchbendRange(mPitchBendRange);
         }
      });

      final String[] lon = {"Main Channel", "Single Channel", "Main and Single Channels", "Disabled"};
      final SettableEnumValue lightsOnNotes =
         host.getPreferences().getEnumSetting("Send MIDI on note playback", "Playback", lon, lon[2]);

      lightsOnNotes.addValueObserver(newValue ->
      {
         if (newValue.equalsIgnoreCase(lon[0])) {
            mSendOnUser = false;
            mSendOnMain = true;
         } else if (newValue.equalsIgnoreCase(lon[1])) {
            mSendOnUser = true;
            mSendOnMain = false;
         } else if (newValue.equalsIgnoreCase(lon[2])) {
            mSendOnUser = true;
            mSendOnMain = true;
         } else {
            mSendOnUser = false;
            mSendOnMain = false;
         }
      });

      final SettableRangedValue lightsMidi =
         host.getPreferences().getNumberSetting("MIDI Out Channel", "Playback", 1, 16, 1, "", 1);

      lightsMidi.addRawValueObserver(ch ->
      {
         host.println("MIDI playback channel: " + ch);
         clearNotes();
         channel = (int)ch;
      });

      host.scheduleTask(() ->
      {
         mDidRunInitTask = true;

         if (mShouldSendInit)
         {
            sendInitializationMessages();
         }
      }, 2000);
   }

   private void handleNotes(int trackIndex, Track track, final PlayingNote[] playingNotes)
   {
      if (trackIndex == selectedTrack)
      {
         for (int i = 0; i < playingNotes.length; i++)
         {
            PlayingNote note = playingNotes[i];
            getHost().println("Note: " + trackIndex + " " + note.velocity() + " " + note.pitch());
            noteCache[note.pitch()] = NOTE_ON_NEW;
            if (mSendOnUser)
               mMidiOut.sendMidi((MSG_NOTE_ON << 4) | channel, note.pitch(), 127);
            if (mSendOnMain)
               mMidiOut.sendMidi((MSG_NOTE_ON << 4) | 16, note.pitch(), 127);
         }
         for (int i = 0; i < this.noteCache.length; i++)
         {
            if (this.noteCache[i] == NOTE_ON_NEW)
               this.noteCache[i] = NOTE_ON;
            else if (this.noteCache[i] == NOTE_ON)
            {
               this.noteCache[i] = NOTE_OFF;
               getHost().println("NoteOff: " + trackIndex + " " + i);
               if (mSendOnUser)
                  mMidiOut.sendMidi((MSG_NOTE_OFF << 4) | channel, i, 0);
               if (mSendOnMain)
                  mMidiOut.sendMidi((MSG_NOTE_OFF << 4) | 16, i, 0);
            }
         }
      }
   }

   private void clearNotes() {
      for (int i = 0; i < this.noteCache.length; i++)
      {
         if (noteCache[i] != NOTE_OFF)
         {
            this.noteCache[i] = NOTE_OFF;
            getHost().println("AllNoteOff: " + i);

            if (mSendOnUser)
                  mMidiOut.sendMidi((MSG_NOTE_OFF << 4) | channel, i, 0);
            if (mSendOnMain)
               mMidiOut.sendMidi((MSG_NOTE_OFF << 4) | 16, i, 0);
         }
      }
   }

   private void callback(final int i, final int i1, final int i2)
   {

      //getHost().println("MidiCallback: " + i + " " + i1 + " " + i2);
   }

   void sendInitializationMessages()
   {
      final MidiOut midiOut = getHost().getMidiOutPort(0);
      // Set up MPE mode: Zone 1 15 channels
      midiOut.sendMidi(0xB0, 101, 0); // Registered Parameter Number (RPN) - MSB*
      midiOut.sendMidi(0xB0, 100, 6); // Registered Parameter Number (RPN) - LSB*
      midiOut.sendMidi(0xB0, 6, 15);
      midiOut.sendMidi(0xB0, 38, 0);

      // Set up MPE mode: Zone 2 off
      midiOut.sendMidi(0xBF, 101, 0);
      midiOut.sendMidi(0xBF, 100, 6);
      midiOut.sendMidi(0xBF, 6, 0);
      midiOut.sendMidi(0xBF, 38, 0);
   }

   void sendPitchbendRange(int range)
   {
      final MidiOut midiOut = getHost().getMidiOutPort(0);

      // Set up Pitch bend range
      midiOut.sendMidi(0xB0, 101, 0); // Registered Parameter Number (RPN) - MSB*
      midiOut.sendMidi(0xB0, 100, 0); // Registered Parameter Number (RPN) - LSB*
      midiOut.sendMidi(0xB0, 6, range);
      midiOut.sendMidi(0xB0, 38, 0);
   }

   @Override
   public void exit()
   {
   }

   @Override
   public void flush()
   {
   }

   private boolean mShouldSendInit = false;
   private boolean mDidRunInitTask = false;
   private int mPitchBendRange = 48;
   private MidiOut mMidiOut;
   private boolean mSendOnMain;
   private boolean mSendOnUser;
}
