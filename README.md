# LinnAlt
## An alternative LinnStrument controller script

A simple Bitwig extension that extends support for Roger Linn's LinnStrument MIDI controller.

The key added feature is the ability for Bitwig to send notes back to the LinnStrument on playback. Notes are sent using the MIDI out port selected for the LinnStrument.

A user can select a combination of MIDI Main Channel (used by LinnStrument in OneChan mode), in a single user-configurable MIDI channel (for the ChanPerNote mode), or any combination of the former. Currently there is no support for ChanPerRow mode, altough it can be added if requested.

## Installation
Copy the .bwextension file to Documents/Bitwig Studio/Extensions. 
If you already have the Linnstrument configured in Bitwig, delete the controller configuration and select "+ Linnstrument (alternative)" when prompted. 
