import {
  IsString,
  IsNotEmpty,
  MinLength,
  MaxLength,
  IsArray,
  IsOptional,
  IsBoolean,
  ArrayMaxSize,
} from 'class-validator';

export class CreateArticleDto {
  @IsString()
  @IsNotEmpty()
  @MinLength(3)
  @MaxLength(200)
  title: string;

  @IsString()
  @IsNotEmpty()
  @MinLength(10)
  @MaxLength(50000)
  content: string;

  @IsString()
  @IsNotEmpty()
  @MinLength(2)
  @MaxLength(100)
  author: string;

  @IsArray()
  @IsOptional()
  @IsString({ each: true })
  @ArrayMaxSize(10)
  tags?: string[];

  @IsBoolean()
  @IsOptional()
  published?: boolean;
}
