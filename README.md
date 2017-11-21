# Ctrl-F
## Inspiration
Ctrl-F is a program that allows for the user to find text quickly and easily in the real world. When browsing the internet or computer documents, we generally use the Ctrl-F command to quickly search through all the text. This command quickly analyzes the text and highlights results that match the input string given by the user (the term that the user wants to find). Such a command inspired me to create an application that will essentially create the Ctrl-F command in real life. This application can be used to search textbook pages for key words, search ingredient lists for specific chemicals, etc.

## What it does
Ctrl-F allows users to specify a search term that they would like to find in a real world document. Initially, the user is shown a live feed of the Android camera and they must take a picture. Next, the user enters the search term they would like to find. The Tesseract OCR (optical character recognition) engine is used to analyze the image and find all valid text in the image. Then, the desired words are highlighted on the image if they are found. Finally, this annotated image is shown to the user. Additionally, the original and annotated images are saved to the user’s device if they need to be accessed again in the future.

## Challenges I ran into
* Finding a viable engine/API (application program interface) was extremely difficult. Many online APIs had rate limits for free use, meaning I would need to pay in order to obtain additional API credits for testing the application. In the end, I decided to use Tesseract as it was quite reliable. It was also an offline engine meaning that it did not need any API credits (only needed training data to be loaded onto the Android device). 
* Figuring out how Tesseract worked was challenging. It was my first time working on a project dealing with OCR, meaning I had to learn a lot about the terminology and functions defined by the Tesseract library.
* At first, the camera orientation was incorrect as the default orientation is landscape. Thus, I tried setting the camera orientation to portrait. However, this made the camera preview look extremely weird if the device was rotated. As a result, I overrode the configuration listener in the Android activity allowing me to detect when the user rotates their device and set my camera orientation accordingly. 

## What I learned
* OCR is difficult. Extremely difficult. 
* Android is a pain. Android Studio is even more of a pain. 
* Learned how to use XML layouts to create a graphical user interface (GUI) in Android.
* Learned about Android Activities, Intents, Permissions, etc. 

## What's next for Ctrl-F
* I hope to publish this application in the Google Play store during the summer, after polishing it up.
* Allowing user to search for other terms after initial search (without retaking image).
* Cleaner user interface (implement Material design concepts).
* Search image for words in real-time (updates the image each time the user types in a letter).
