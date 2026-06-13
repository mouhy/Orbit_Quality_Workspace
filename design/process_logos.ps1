# Trim + resize logos
Add-Type -AssemblyName System.Drawing

function Process-Logo($src, $dst, $maxW) {
    $img = [System.Drawing.Bitmap]::FromFile($src)
    $rect = New-Object System.Drawing.Rectangle(0, 0, $img.Width, $img.Height)
    $data = $img.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $bytes = New-Object byte[] ($data.Stride * $img.Height)
    [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
    $img.UnlockBits($data)

    $minX = $img.Width; $minY = $img.Height; $maxX = 0; $maxY = 0
    for ($y = 0; $y -lt $img.Height; $y++) {
        $row = $y * $data.Stride
        for ($x = 0; $x -lt $img.Width; $x++) {
            $a = $bytes[$row + $x * 4 + 3]
            if ($a -gt 8) {
                if ($x -lt $minX) { $minX = $x }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    $pad = [int](($maxX - $minX) * 0.03)
    $minX = [Math]::Max(0, $minX - $pad); $minY = [Math]::Max(0, $minY - $pad)
    $maxX = [Math]::Min($img.Width - 1, $maxX + $pad); $maxY = [Math]::Min($img.Height - 1, $maxY + $pad)
    $w = $maxX - $minX + 1; $h = $maxY - $minY + 1

    $scale = [Math]::Min(1.0, $maxW / $w)
    $nw = [int]($w * $scale); $nh = [int]($h * $scale)

    $out = New-Object System.Drawing.Bitmap($nw, $nh, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($out)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $srcRect = New-Object System.Drawing.Rectangle($minX, $minY, $w, $h)
    $dstRect = New-Object System.Drawing.Rectangle(0, 0, $nw, $nh)
    $g.DrawImage($img, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()
    $out.Save($dst, [System.Drawing.Imaging.ImageFormat]::Png)
    $size = (Get-Item $dst).Length
    Write-Output "$([System.IO.Path]::GetFileName($dst)) : ${nw}x${nh} ($size bytes)"
    $out.Dispose(); $img.Dispose()
}

$logos = "E:\Orbit_Mobile_Native\design\logos"
$draw = "E:\Orbit_Mobile_Native\app\src\main\res\drawable-nodpi"
New-Item -ItemType Directory -Force $draw | Out-Null

Process-Logo "$logos\slogo.png" "$draw\orbit_logo_mark.png" 640
Process-Logo "$logos\logo__1_.png" "$draw\orbit_logo_full_dark.png" 800
Process-Logo "$logos\llogo.png" "$draw\orbit_logo_full_light.png" 800
Process-Logo "$logos\lilogo.png" "$draw\orbit_logo_silver.png" 800
