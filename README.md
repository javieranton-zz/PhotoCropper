# PhotoCropper
Crop and resize your images in CodenameOne

Sample usage: (resizes to 400x400 and checks for a minimum original image size of 192x192)

`com.javieranton.PhotoCropper.cropImage(originalImg, 192, 400, 400, croppedImg -> {   
//DO SOMETHING WITH YOUR CROPPED IMAGE
}, "Crop your image", "Warning message image is too small");`
